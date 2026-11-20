package fr.clrk.reveil.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import fr.clrk.reveil.R
import fr.clrk.reveil.ReveilApp

/**
 * Android answers the install here. When it needs the person to confirm (always before Android 12), its screen
 * opens right away if the person asked for the install; in the background nothing pops up by itself:
 * a quiet notification offers to install, and so does the list.
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) return
        val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
        if (intent.getBooleanExtra(BACKGROUND, false)) notify(context, confirm, intent.getStringExtra(VERSION).orEmpty())
        else context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun notify(context: Context, confirm: Intent, version: String) {
        val open = PendingIntent.getActivity(context, 0, confirm, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ReveilApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Nouvelle version $version prête")
            .setContentText("Touchez pour l'installer")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification)
    }

    companion object {
        const val BACKGROUND = "background"
        const val VERSION = "version"
        private const val NOTIFICATION = 7
    }
}
