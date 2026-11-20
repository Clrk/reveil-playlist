package fr.clrk.reveil.update

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import fr.clrk.reveil.core.Releases
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.music.NewPipeDownloader
import fr.clrk.reveil.text

/**
 * Looks for a newer version on GitHub. The app never installs anything itself (Play Protect flags apps that can):
 * the browser downloads the APK and Android installs it, as for the first install.
 */
class Updater(private val context: Context) {
    private val installed get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** Blocking: the newer version published on GitHub, if any. */
    fun check(): Update? =
        http.text(Releases.LATEST, mapOf("Accept" to "application/vnd.github+json"))?.let { Releases.newer(installed, it) }

    /** What opens the APK's download in the browser. */
    fun download(update: Update): Intent = Intent(Intent.ACTION_VIEW, update.apk.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun open(update: Update) = context.startActivity(download(update))

    private companion object {
        val http get() = NewPipeDownloader.instance.client
    }
}
