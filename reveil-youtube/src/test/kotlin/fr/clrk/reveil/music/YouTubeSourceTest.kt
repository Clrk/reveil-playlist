package fr.clrk.reveil.music

import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.Track
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YouTubeSourceTest {
    private val track = Track("abc", "La mer")
    private val source = YouTubeSource({ listOf(track) }, { "https://flux/${it.id}" }, { listOf(track) })

    @Test
    fun `each part of the source can be swapped`() {
        assertEquals(listOf(track), source.tracks(PlaylistId("PLx")))
        assertEquals("https://flux/abc", source.streamUrl(track))
        assertEquals(listOf(track), source.search("la mer"))
    }
}
