package fr.clrk.reveil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The newer version found on GitHub, as the list shows it, and its install. */
class Upgrade(private val updater: Updater, private val scope: CoroutineScope) {
    private var found by mutableStateOf<Update?>(null)
    private var downloading by mutableStateOf<Float?>(null)

    val note get() = found?.let { UpdateNote(it.version, downloading) }

    fun check() { scope.launch(Dispatchers.IO) { found = updater.check() } }

    /** Android first asks once to allow the app to install its updates; then it downloads and installs. */
    fun install() {
        val update = found ?: return
        if (!updater.allowed()) return updater.askPermission()
        downloading = 0f
        scope.launch(Dispatchers.IO) { if (!updater.install(update) { downloading = it }) downloading = null }
    }
}
