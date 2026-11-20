package fr.clrk.reveil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.clrk.reveil.core.Update
import fr.clrk.reveil.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The newer version found on GitHub, as the list shows it; "Installer" downloads it in the browser. */
class Upgrade(private val updater: Updater, private val scope: CoroutineScope) {
    private var found by mutableStateOf<Update?>(null)

    val note get() = found?.let { UpdateNote(it.version) }

    fun check() { scope.launch(Dispatchers.IO) { found = updater.check() } }

    fun install() { found?.let(updater::open) }
}
