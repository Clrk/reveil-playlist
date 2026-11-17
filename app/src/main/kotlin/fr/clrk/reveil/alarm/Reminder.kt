package fr.clrk.reveil.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import fr.clrk.reveil.R
import fr.clrk.reveil.ReveilApp
import fr.clrk.reveil.app
import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.ui.MainActivity
import java.time.format.DateTimeFormatter

/** The quiet notification shown an hour before ringing, with a way to skip this wake-up. */
object Reminder {
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun show(context: Context, alarm: Alarm) {
        val time = alarm.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        val title = listOf("Réveil à $time", alarm.label).filter { it.isNotBlank() }.joinToString(" · ")
        val skip = Intent(context, SkipReceiver::class.java).setAction("fr.clrk.reveil.SKIP.${alarm.id}")
            .putExtra(AlarmReceiver.EXTRA_ID, alarm.id)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), FLAGS)
        val notification = NotificationCompat.Builder(context, ReveilApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(alarm.playlistTitle?.let { "♪ $it" } ?: "Sonnerie du téléphone")
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Ignorer ce réveil", PendingIntent.getBroadcast(context, 0, skip, FLAGS))
            .build()
        manager(context).notify(alarm.id.hashCode(), notification)
    }

    fun hide(context: Context, alarmId: String) = manager(context).cancel(alarmId.hashCode())

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
}

/** « Ignorer ce réveil » from the reminder: skips this occurrence only. */
class SkipReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmReceiver.EXTRA_ID) ?: return
        context.app.alarms.skipNext(id)
        context.app.alarmsChanged()
        Reminder.hide(context, id)
    }
}
