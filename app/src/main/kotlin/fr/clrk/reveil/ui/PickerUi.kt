package fr.clrk.reveil.ui

import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.SearchResult

/** What the playlist picker displays. */
data class PickerUi(
    val query: String = "",
    val result: SearchResult = SearchResult.Idle,
    val loading: Boolean = false,
    val tab: PickerTab = PickerTab.MINE,
    val library: LibraryUi = LibraryUi.NotConnected,
)

enum class PickerTab(val label: String) { MINE("Mes playlists"), SEARCH("Rechercher") }

sealed interface LibraryUi {
    data object NotConnected : LibraryUi

    data object Loading : LibraryUi

    data class Loaded(val playlists: List<PlaylistChoice>) : LibraryUi

    data class Failed(val message: String) : LibraryUi
}

/** What the picker can ask for. */
interface PickerEvents {
    fun query(text: String)
    fun tab(tab: PickerTab)
    fun pick(choice: PlaylistChoice)
    fun signIn()
    fun signOut()
    fun close()
}
