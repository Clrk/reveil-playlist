package fr.clrk.reveil.music

import fr.clrk.reveil.core.MusicSource
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.StreamResolver
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.TrackSearch
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Title search: YouTube Music songs first, plain YouTube videos when YouTube Music has nothing. */
object NewPipeTrackSearch : TrackSearch {
    override fun search(query: String): List<Track> = YouTubeHealth.watch {
        searchIn(query, YoutubeSearchQueryHandlerFactory.MUSIC_SONGS).ifEmpty {
            searchIn(query, YoutubeSearchQueryHandlerFactory.VIDEOS)
        }
    }

    private fun searchIn(query: String, filter: String): List<Track> {
        val service = ServiceList.YouTube
        val extractor = service.getSearchExtractor(query, listOf(filter), "")
        extractor.fetchPage()
        return extractor.initialPage.items.filterIsInstance<StreamInfoItem>()
            .map { Track(service.streamLHFactory.getId(it.url), it.name) }
    }
}

/** YouTube through NewPipeExtractor, as one replaceable [MusicSource]. */
class YouTubeSource(
    playlists: PlaylistSource = NewPipePlaylistSource(),
    streams: StreamResolver = AudioResolver,
    search: TrackSearch = NewPipeTrackSearch,
) : MusicSource, PlaylistSource by playlists, StreamResolver by streams, TrackSearch by search
