package fr.clrk.reveil.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.clrk.reveil.R
import fr.clrk.reveil.alarm.RingChannels
import fr.clrk.reveil.display.Update
import fr.clrk.reveil.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** A few times a day: when a newer version is out, a quiet notification offers it, once per version. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val updater = Updater(applicationContext)
        updater.check()?.takeIf { it.version != prefs.getString(NOTIFIED, null) }?.let(::notify)
        Result.success()
    }

    /** Touching it opens the app, whose banner downloads the new version. */
    private fun notify(update: Update) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val app = Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(applicationContext, 0, app, flags)
        val notification = NotificationCompat.Builder(applicationContext, RingChannels.REMINDER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Nouvelle version ${update.version} disponible")
            .setContentText("Touchez pour l'installer")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification)
        prefs.edit { putString(NOTIFIED, update.version) }
    }

    companion object {
        private const val WORK = "update"
        private const val NOTIFIED = "notified"
        private const val NOTIFICATION = 7
        private const val EVERY_HOURS = 6L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(EVERY_HOURS, TimeUnit.HOURS).setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
