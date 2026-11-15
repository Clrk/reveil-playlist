package fr.clrk.reveil.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.clrk.reveil.app

/** Fired by AlarmManager: moves the alarm to its next occurrence and starts ringing. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        context.app.alarms.onRing(id)
        context.app.alarmsChanged()
        RingService.start(context, id)
    }

    companion object {
        const val EXTRA_ID = "alarmId"
    }
}

/** AlarmManager forgets everything on reboot, time or zone change: plan again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        context.app.alarms.rescheduleAll()
        context.app.alarmsChanged()
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
