package fr.clrk.reveil.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import fr.clrk.reveil.core.AlarmScheduler
import fr.clrk.reveil.core.TextStore
import fr.clrk.reveil.core.TriggerKey
import java.io.File
import java.time.Instant

/** Exact, Doze-proof alarms through AlarmManager.setAlarmClock (shown in the status bar). */
class AndroidAlarmScheduler(private val context: Context, private val home: Class<out Activity>) : AlarmScheduler {
    private val manager = context.getSystemService(AlarmManager::class.java)

    /** Reminders are plain exact alarms: only real wake-ups appear as the status-bar alarm clock. */
    override fun schedule(key: TriggerKey, at: Instant) {
        if (key.kind == TriggerKey.Kind.REMINDER) {
            return manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation(key))
        }
        val show = PendingIntent.getActivity(context, 0, Intent(context, home), FLAGS)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), operation(key))
    }

    override fun cancel(key: TriggerKey) = manager.cancel(operation(key))

    private fun operation(key: TriggerKey): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction("fr.clrk.reveil.${key.kind}.${key.alarmId}")
            .putExtra(AlarmReceiver.EXTRA_ID, key.alarmId)
            .putExtra(AlarmReceiver.EXTRA_KIND, key.kind.name)
        return PendingIntent.getBroadcast(context, 0, intent, FLAGS)
    }

    private companion object {
        const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}

/** Stores the alarms JSON in a file, written atomically. */
class FileTextStore(private val file: File) : TextStore {
    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        temp.renameTo(file)
    }
}
