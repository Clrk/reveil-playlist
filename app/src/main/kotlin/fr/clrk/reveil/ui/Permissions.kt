package fr.clrk.reveil.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/** One thing Android must allow for the alarm to work, with why and how to fix it. */
data class PermissionStep(val title: String, val why: String, val warning: String, val granted: Boolean, val fix: () -> Unit)

/** The permissions a reliable alarm needs, read fresh from the system each time. */
class Permissions(private val activity: Activity) {
    private val notifications = activity.getSystemService(NotificationManager::class.java)

    fun steps(): List<PermissionStep> = listOfNotNull(notificationStep(), fullScreenStep(), batteryStep())

    fun warnings() = steps().filterNot { it.granted }.map { it.warning to it.fix }

    private fun notificationStep() = PermissionStep(
        "Notifications", "Pour sonner et afficher le rappel une heure avant.",
        "Notifications désactivées : le réveil ne pourra pas s'afficher",
        notifications.areNotificationsEnabled(),
    ) { openSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS) }

    private fun fullScreenStep() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        null
    } else {
        PermissionStep(
            "Plein écran", "Pour afficher le réveil par-dessus l'écran verrouillé.",
            "Autoriser l'affichage plein écran sur l'écran verrouillé",
            notifications.canUseFullScreenIntent(),
        ) { openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT) }
    }

    private fun batteryStep() = PermissionStep(
        "Batterie", "Pour télécharger tes musiques la veille, même téléphone en veille.",
        "L'économie de batterie peut empêcher de préparer les musiques hors ligne",
        activity.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(activity.packageName),
        ::askBatteryExemption,
    )

    private fun openSettings(action: String) {
        val intent = Intent(action).setData("package:${activity.packageName}".toUri())
        activity.startActivity(intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
    }

    /** Alarm clocks are an accepted use of this request: without it, the overnight download can be frozen. */
    @SuppressLint("BatteryLife")
    private fun askBatteryExemption() = activity.startActivity(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${activity.packageName}".toUri()),
    )
}
