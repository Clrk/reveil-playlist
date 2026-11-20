package fr.clrk.reveil.music

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.FallbackPlaylistSource
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PrefetchOutcome
import fr.clrk.reveil.core.PrefetchPolicy
import fr.clrk.reveil.core.PrefetchReport
import fr.clrk.reveil.core.PrefetchResult
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.WakeInfo
import fr.clrk.reveil.weather.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Downloads a rotating handful of tracks the evening before the next alarm. */
class PrefetchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        WeatherRepository(applicationContext).refresh()
        val playlist = PlaylistId(inputData.getString(KEY_PLAYLIST) ?: return@withContext Result.failure())
        val source = FallbackPlaylistSource(YtMusicAccount(applicationContext), NewPipePlaylistSource())
        val tracks = runCatching { source.tracks(playlist) }.getOrNull() ?: return@withContext report(null, 0)
        refresh(TrackCache(applicationContext, playlist), tracks)
    }

    private suspend fun refresh(cache: TrackCache, tracks: List<Track>): Result {
        val plan = PrefetchPolicy.plan(tracks, cache.tracks(), cache.played(), CAPACITY, Random.Default, cache.known())
        plan.evict.forEach(cache::remove)
        val stored = store(plan.download, cache)
        if (cache.played().size >= tracks.size - CAPACITY) cache.forgetPlayed()
        val report = PrefetchOutcome.of(plan.download.size, stored, byHand)
        if (report.upToDate) cache.remember(tracks)
        return result(report)
    }

    /** Downloads each track, reporting progress; returns how many succeeded. */
    private suspend fun store(tracks: List<Track>, cache: TrackCache): Int = tracks.withIndex().count { (index, track) ->
        setProgress(workDataOf(KEY_DONE to index, KEY_TOTAL to tracks.size))
        runCatching { cache.store(track, AudioResolver.streamUrl(track)) }.isSuccess
    }

    private val byHand get() = inputData.getBoolean(KEY_NOW, false)

    private fun report(planned: Int?, stored: Int) = result(PrefetchOutcome.of(planned, stored, byHand))

    private fun result(report: PrefetchReport): Result = when (report.result) {
        PrefetchResult.DONE -> Result.success()
        PrefetchResult.RETRY_LATER -> Result.retry()
        PrefetchResult.FAILED -> Result.failure()
    }

    companion object {
        const val KEY_PLAYLIST = "playlist"
        const val KEY_NOW = "now"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val CAPACITY = 15
    }
}

object PrefetchScheduler {
    private const val WORK = "prefetch"
    private const val WORK_NOW = "prefetch-now"

    fun reschedule(context: Context, next: Pair<Alarm, ZonedDateTime>?) {
        val playlist = next?.first?.playlistId ?: return WorkManager.getInstance(context).cancelUniqueWork(WORK).let { }
        val now = ZonedDateTime.now()
        enqueue(context, WORK, playlist, Duration.between(now, PrefetchPolicy.at(next.second, now)))
    }

    /** Uses its own slot, so it never replaces the evening download of the next alarm. */
    fun now(context: Context, playlist: String) = enqueue(context, WORK_NOW, playlist, Duration.ZERO)

    /** State of the hand-started download, null when there is none to show. */
    fun nowState(context: Context): Flow<WakeInfo.Download?> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(WORK_NOW).map { it.firstOrNull()?.let(::toDownload) }

    private fun toDownload(info: WorkInfo): WakeInfo.Download? = when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> WakeInfo.Download.Waiting
        WorkInfo.State.RUNNING -> running(info)
        WorkInfo.State.FAILED -> WakeInfo.Download.Failed
        else -> null
    }

    private fun running(info: WorkInfo) = WakeInfo.Download.Running(
        info.progress.getInt(PrefetchWorker.KEY_DONE, 0),
        info.progress.getInt(PrefetchWorker.KEY_TOTAL, 0),
    )

    private fun enqueue(context: Context, name: String, playlist: String, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<PrefetchWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(PrefetchWorker.KEY_PLAYLIST to playlist, PrefetchWorker.KEY_NOW to (name == WORK_NOW)))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }
}
