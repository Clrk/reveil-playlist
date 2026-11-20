package fr.clrk.reveil.music

import fr.clrk.reveil.core.FoundPlaylist
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSearch
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.Track
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit

/** HTTP bridge NewPipeExtractor needs, backed by OkHttp. */
class NewPipeDownloader private constructor(val client: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()
        val builder = Builder().url(request.url()).method(request.httpMethod(), body).header("User-Agent", USER_AGENT)
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        client.newCall(builder.build()).execute().use { response ->
            if (response.code == TOO_MANY_REQUESTS) throw ReCaptchaException("reCaptcha", request.url())
            val headers = response.headers.toMultimap()
            return Response(response.code, response.message, headers, response.body.string(), response.request.url.toString())
        }
    }

    companion object {
        private const val TOO_MANY_REQUESTS = 429
        private const val TIMEOUT_SECONDS = 20L
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        val instance = NewPipeDownloader(OkHttpClient.Builder().readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).build())
    }
}

/** Lists a YouTube Music playlist through NewPipeExtractor (no account, no subscription). */
class NewPipePlaylistSource(private val maxTracks: Int = MAX_TRACKS) : PlaylistSource {
    private val service = ServiceList.YouTube

    override fun tracks(id: PlaylistId): List<Track> = YouTubeHealth.watch { list(id) }

    private fun list(id: PlaylistId): List<Track> {
        val info = PlaylistInfo.getInfo(service, id.url)
        val items = info.relatedItems.toMutableList()
        var page: Page? = info.nextPage
        while (Page.isValid(page) && items.size < maxTracks) {
            val more = PlaylistInfo.getMoreItems(service, id.url, page)
            items += more.items
            page = more.nextPage
        }
        return items.take(maxTracks).map(::toTrack)
    }

    private fun toTrack(item: StreamInfoItem) = Track(service.streamLHFactory.getId(item.url), item.name)

    private companion object {
        const val MAX_TRACKS = 1000
    }
}

/** Finds a playable audio stream URL for a track. */
object AudioResolver {
    fun streamUrl(track: Track): String = YouTubeHealth.watch { bestStream(track) }

    private fun bestStream(track: Track): String {
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=${track.id}")
        val best = info.audioStreams.filter { it.isUrl }.maxWithOrNull(compareBy<AudioStream>({ it.isM4a() }, { it.averageBitrate }))
        return requireNotNull(best) { "Aucun flux audio pour ${track.id}" }.content
    }

    private fun AudioStream.isM4a() = format?.suffix == "m4a"
}

/** YouTube Music playlist search (community and featured playlists), no account needed. */
object NewPipePlaylistSearch : PlaylistSearch {
    override fun search(query: String): List<FoundPlaylist> =
        searchIn(query, YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS).ifEmpty {
            searchIn(query, YoutubeSearchQueryHandlerFactory.PLAYLISTS)
        }

    /** YouTube Music first; plain YouTube playlists when YouTube Music has nothing (e.g. French queries). */
    private fun searchIn(query: String, filter: String): List<FoundPlaylist> {
        val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(filter), "")
        extractor.fetchPage()
        return extractor.initialPage.items.filterIsInstance<PlaylistInfoItem>().map {
            val cover = it.thumbnails.maxByOrNull { thumbnail -> thumbnail.height }?.url
            FoundPlaylist(it.url, it.name, it.uploaderName.orEmpty(), it.streamCount, cover)
        }
    }

    /** Real name and size of a playlist whose link was pasted. */
    fun describe(choice: PlaylistChoice): PlaylistChoice {
        val info = PlaylistInfo.getInfo(ServiceList.YouTube, choice.id.url)
        return choice.copy(title = info.name, author = info.uploaderName.orEmpty(), trackCount = info.streamCount)
    }
}
