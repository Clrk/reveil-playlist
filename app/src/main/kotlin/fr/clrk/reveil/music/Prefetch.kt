package fr.clrk.reveil.music

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PrefetchPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Downloads a rotating handful of tracks the evening before the next alarm. */
class PrefetchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val playlist = PlaylistId(inputData.getString(KEY_PLAYLIST) ?: return@withContext Result.failure())
        val tracks = runCatching { NewPipePlaylistSource().tracks(playlist) }.getOrNull() ?: return@withContext Result.retry()
        val cache = TrackCache(applicationContext, playlist)
        val plan = PrefetchPolicy.plan(tracks, cache.tracks(), cache.played(), CAPACITY, Random.Default)
        plan.evict.forEach(cache::remove)
        plan.download.forEach { track -> runCatching { cache.store(track, AudioResolver.streamUrl(track)) } }
        if (cache.played().size >= tracks.size - CAPACITY) cache.forgetPlayed()
        Result.success()
    }

    companion object {
        const val KEY_PLAYLIST = "playlist"
        const val CAPACITY = 15
    }
}

object PrefetchScheduler {
    private const val WORK = "prefetch"

    fun reschedule(context: Context, next: Pair<Alarm, ZonedDateTime>?) {
        val playlist = next?.first?.playlistId ?: return WorkManager.getInstance(context).cancelUniqueWork(WORK).let { }
        val now = ZonedDateTime.now()
        enqueue(context, playlist, Duration.between(now, PrefetchPolicy.at(next.second, now)))
    }

    fun now(context: Context, playlist: String) = enqueue(context, playlist, Duration.ZERO)

    private fun enqueue(context: Context, playlist: String, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<PrefetchWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(PrefetchWorker.KEY_PLAYLIST to playlist))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
