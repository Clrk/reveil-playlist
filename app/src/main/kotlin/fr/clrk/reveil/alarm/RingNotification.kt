package fr.clrk.reveil.alarm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import fr.clrk.reveil.R
import fr.clrk.reveil.ReveilApp
import fr.clrk.reveil.ui.RingActivity

/** The ringing notification: full screen over the lock screen, with Snooze and Arrêter. */
internal fun Context.ringNotification(label: String?, canSnooze: Boolean): Notification {
    val fullScreen = PendingIntent.getActivity(this, 0, Intent(this, RingActivity::class.java), RingService.FLAGS)
    return NotificationCompat.Builder(this, ReveilApp.CHANNEL_RING)
        .setSmallIcon(R.drawable.ic_alarm)
        .setContentTitle(label?.ifBlank { null } ?: "Réveil")
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setOngoing(true)
        .setFullScreenIntent(fullScreen, true)
        .apply { if (canSnooze) addAction(0, "Snooze", action(RingService.ACTION_SNOOZE)) }
        .addAction(0, "Arrêter", action(RingService.ACTION_DISMISS))
        .build()
}

private fun Context.action(name: String) =
    PendingIntent.getService(this, name.hashCode(), Intent(this, RingService::class.java).setAction(name), RingService.FLAGS)
