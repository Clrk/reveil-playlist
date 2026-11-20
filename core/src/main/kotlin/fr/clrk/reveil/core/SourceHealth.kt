package fr.clrk.reveil.core

/** What came back from YouTube: music, a page the extractor could not read, or no network at all. */
enum class SourceEvent { WORKED, UNREADABLE, OFFLINE }

/**
 * Whether YouTube still works with the app. A network cut says nothing about it; [BROKEN_AFTER]
 * unreadable answers in a row mean YouTube changed, until it answers normally again.
 */
data class SourceHealth(val unreadable: Int = 0) {
    fun after(event: SourceEvent): SourceHealth = when (event) {
        SourceEvent.WORKED -> SourceHealth()
        SourceEvent.UNREADABLE -> SourceHealth(unreadable + 1)
        SourceEvent.OFFLINE -> this
    }

    val broken: Boolean get() = unreadable >= BROKEN_AFTER

    private companion object {
        const val BROKEN_AFTER = 2
    }
}
