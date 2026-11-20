package fr.clrk.reveil.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.clrk.reveil.alarm.RingState
import fr.clrk.reveil.app
import fr.clrk.reveil.core.SilentUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * A few times a day, on Wi-Fi: installs a newer version unattended (Android 12+), never close to a wake-up.
 * Before Android 12 Android needs a tap: the version is downloaded and a notification offers to install it.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val updater = Updater(applicationContext)
        val update = updater.check()
        if (update != null && updater.allowed() && quiet()) updater.install(update, background = true) { }
        Result.success()
    }

    private fun quiet(): Boolean {
        val alarms = applicationContext.app.alarms
        val upcoming = listOfNotNull(alarms.next()?.second?.toInstant()) + alarms.list().mapNotNull { it.snoozedUntil }
        return SilentUpdate.allowed(Instant.now(), upcoming, ringing = RingState.ui.value != null)
    }

    companion object {
        private const val WORK = "update"
        private const val EVERY_HOURS = 6L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build()
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(EVERY_HOURS, TimeUnit.HOURS).setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
