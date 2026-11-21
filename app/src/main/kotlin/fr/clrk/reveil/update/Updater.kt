package fr.clrk.reveil.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import fr.clrk.reveil.core.Download
import fr.clrk.reveil.core.Releases
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.core.UpdateBanner
import fr.clrk.reveil.music.NewPipeDownloader
import fr.clrk.reveil.text

/**
 * Looks for a newer version on GitHub and downloads it. The app never installs anything itself (Play Protect flags
 * apps that can): Android installs the APK when it is opened from the downloads, as for the first install.
 */
class Updater(private val context: Context) {
    private val installed get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    private val downloads = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    /** Blocking: the newer version published on GitHub, if any. */
    fun check(): Update? =
        http.text(Releases.LATEST, mapOf("Accept" to "application/vnd.github+json"))?.let { Releases.newer(installed, it) }

    /** Downloads the APK into the phone's Downloads, with Android's notification. Before Android 10, the browser does it. */
    fun start(update: Update) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return context.startActivity(browser(update))
        val request = DownloadManager.Request(update.apk.toUri())
            .setTitle("Réveil Playlist ${update.version}")
            .setMimeType(APK)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, UpdateBanner.fileName(update.version))
        prefs.edit { putLong(key(update), downloads.enqueue(request)) }
    }

    /** Blocking: where the download of [update] stands, or null when none was started or it was removed. */
    fun progress(update: Update): Download? {
        val id = prefs.getLong(key(update), NONE).takeIf { it != NONE } ?: return null
        return downloads.query(DownloadManager.Query().setFilterById(id)).use { if (it.moveToFirst()) state(it) else null }
    }

    /** The phone's downloads, where touching the APK installs it. */
    fun openDownloads() =
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun browser(update: Update) = Intent(Intent.ACTION_VIEW, update.apk.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun state(row: Cursor): Download = when (row.int(DownloadManager.COLUMN_STATUS)) {
        DownloadManager.STATUS_SUCCESSFUL -> Download.Done
        DownloadManager.STATUS_FAILED -> Download.Failed
        else -> Download.Running(
            row.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
            row.long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
        )
    }

    private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))

    private fun key(update: Update) = "download-${update.version}"

    private companion object {
        const val APK = "application/vnd.android.package-archive"
        const val NONE = -1L
        val http get() = NewPipeDownloader.instance.client
    }
}
