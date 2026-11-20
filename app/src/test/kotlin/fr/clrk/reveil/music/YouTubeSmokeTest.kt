package fr.clrk.reveil.music

import fr.clrk.reveil.core.PlaylistLink
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URI

/**
 * Talks to the real YouTube, as the alarm does: search, list a playlist, find an audio stream.
 * Skipped unless run with `-Psmoke`: the daily workflow uses it to notice when YouTube breaks NewPipeExtractor.
 */
class YouTubeSmokeTest {
    @Test
    fun aPlaylistFoundBySearchCanBePlayed() {
        val found = NewPipePlaylistSearch.search(QUERY)
        assertTrue("aucune playlist trouvée", found.isNotEmpty())
        val playlist = requireNotNull(PlaylistLink.parse(found.first().url)) { "lien illisible : ${found.first().url}" }
        val tracks = NewPipePlaylistSource(MAX_TRACKS).tracks(playlist)
        assertTrue("playlist vide", tracks.isNotEmpty())
        assertTrue("aucun flux audio", AudioResolver.streamUrl(tracks.first()).startsWith("https://"))
    }

    companion object {
        private const val QUERY = "lofi morning"
        private const val MAX_TRACKS = 20

        @BeforeClass
        @JvmStatic
        fun online() {
            assumeTrue(System.getProperty("smoke") == "true")
            NewPipe.init(PlainDownloader)
        }
    }
}

/** OkHttp believes it runs on Android in unit tests, so the smoke test talks HTTP through the JDK. */
private object PlainDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val connection = URI(request.url()).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = request.httpMethod()
        connection.setRequestProperty("User-Agent", NewPipeDownloader.USER_AGENT)
        request.headers().forEach { (name, values) -> values.forEach { connection.addRequestProperty(name, it) } }
        request.dataToSend()?.let { send(connection, it) }
        val body = read(connection)
        val headers = connection.headerFields.filterKeys { it != null }
        return Response(connection.responseCode, connection.responseMessage, headers, body, connection.url.toString())
    }

    private fun send(connection: HttpURLConnection, body: ByteArray) {
        connection.doOutput = true
        connection.outputStream.use { it.write(body) }
    }

    private fun read(connection: HttpURLConnection): String {
        val failed = connection.responseCode >= HttpURLConnection.HTTP_BAD_REQUEST
        val stream = if (failed) connection.errorStream else connection.inputStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
}
