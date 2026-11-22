package fr.clrk.reveil.core.bdd

import fr.clrk.reveil.core.TextStore

/** In-memory [TextStore] for scenarios. */
class MemoryStore : TextStore {
    var content: String? = null

    override fun read() = content

    override fun write(text: String) { content = text }
}
