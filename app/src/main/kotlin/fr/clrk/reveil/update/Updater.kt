package fr.clrk.reveil.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.net.toUri
import android.provider.Settings
import fr.clrk.reveil.core.Releases
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.music.NewPipeDownloader
import fr.clrk.reveil.text
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Looks for a newer version on GitHub and installs it: the app is not on the Play Store. */
class Updater(private val context: Context) {
    private val installed get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** Blocking: the newer version published on GitHub, if any. */
    fun check(): Update? =
        http.text(Releases.LATEST, mapOf("Accept" to "application/vnd.github+json"))?.let { Releases.newer(installed, it) }

    /** Android asks once to allow this app to install its own updates. */
    fun allowed(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun askPermission() {
        val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
        context.startActivity(settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Blocking: downloads the APK, reporting [progress] from 0 to 1, then hands it to Android. Android installs it
     * without asking when the app installed itself last time (Android 12+); otherwise it asks the person on its own
     * screen, except in the [background], where the list keeps offering the update instead.
     * False when the download or the hand-over failed.
     */
    fun install(update: Update, background: Boolean = false, progress: (Float) -> Unit): Boolean = runCatching {
        val apk = download(update, progress)
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        installer.openSession(installer.createSession(params)).use { session ->
            session.openWrite("update.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) } }
            session.commit(done(background).intentSender)
        }
        true
    }.getOrDefault(false)

    private fun download(update: Update, progress: (Float) -> Unit): File {
        val file = File(context.cacheDir, "update.apk")
        http.newCall(Request.Builder().url(update.apk).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body
            val total = body.contentLength().coerceAtLeast(1)
            body.byteStream().use { input -> file.outputStream().use { out -> copy(input, out, total, progress) } }
        }
        return file
    }

    private fun copy(input: InputStream, out: OutputStream, total: Long, progress: (Float) -> Unit) {
        val buffer = ByteArray(BUFFER)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            copied += read
            progress((copied.toFloat() / total).coerceAtMost(1f))
        }
    }

    private fun done(background: Boolean): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, UpdateReceiver::class.java).putExtra(UpdateReceiver.BACKGROUND, background),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private companion object {
        const val BUFFER = 64 * 1024
        val http get() = NewPipeDownloader.instance.client
    }
}
