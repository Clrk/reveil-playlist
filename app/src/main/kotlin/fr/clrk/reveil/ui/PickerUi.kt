package fr.clrk.reveil.ui

import fr.clrk.reveil.core.SearchResult

/** What the playlist picker displays. */
data class PickerUi(val query: String = "", val result: SearchResult = SearchResult.Idle, val loading: Boolean = false)
