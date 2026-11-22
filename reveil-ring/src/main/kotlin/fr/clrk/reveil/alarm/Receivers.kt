package fr.clrk.reveil.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.clrk.reveil.core.TriggerKey

/** Fired by AlarmManager: shows the reminder, or moves the alarm to its next occurrence and starts ringing. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val kind = intent.getStringExtra(EXTRA_KIND)?.let(TriggerKey.Kind::valueOf) ?: TriggerKey.Kind.RING
        if (kind == TriggerKey.Kind.REMINDER) return remind(context, id)
        Reminder.hide(context, id)
        context.ringHost.alarms.onRing(id, kind)
        context.ringHost.alarmsChanged()
        RingService.start(context, id)
    }

    private fun remind(context: Context, id: String) {
        context.ringHost.alarms.find(id)?.let { Reminder.show(context, it) }
    }

    companion object {
        const val EXTRA_ID = "alarmId"
        const val EXTRA_KIND = "kind"
    }
}

/** AlarmManager forgets everything on reboot, time or zone change: plan again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        context.ringHost.alarms.rescheduleAll()
        context.ringHost.alarmsChanged()
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
