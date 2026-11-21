package fr.clrk.reveil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.clrk.reveil.core.Download
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.core.UpdateBanner
import fr.clrk.reveil.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The newer version found on GitHub, as the list shows it: one button downloads it, then opens it to install. */
class Upgrade(private val updater: Updater, private val scope: CoroutineScope) {
    private var found by mutableStateOf<Update?>(null)
    private var download by mutableStateOf<Download?>(null)

    val banner get() = found?.let { UpdateBanner.of(it.version, download) }

    fun check() {
        scope.launch(Dispatchers.IO) { found = updater.check()?.also { follow(it) } }
    }

    /** The banner's button: download, open the downloaded file, or try again. */
    fun act() {
        val update = found ?: return
        when (download) {
            Download.Done -> updater.openDownloads()
            is Download.Running -> Unit
            else -> scope.launch(Dispatchers.IO) { updater.start(update); follow(update) }
        }
    }

    /** Blocking: refreshes the download's progress until it ends. */
    private suspend fun follow(update: Update) {
        do {
            download = updater.progress(update)
            if (download is Download.Running) delay(POLL_MS)
        } while (download is Download.Running)
    }

    private companion object {
        const val POLL_MS = 500L
    }
}
