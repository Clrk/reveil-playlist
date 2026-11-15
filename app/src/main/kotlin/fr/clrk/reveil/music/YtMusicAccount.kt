package fr.clrk.reveil.music

import android.content.Context
import androidx.core.content.edit
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistId
import fr.clrk.reveil.core.PlaylistSource
import fr.clrk.reveil.core.Track
import fr.clrk.reveil.core.YtMusicParser
import fr.clrk.reveil.core.YtMusicSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/** The signed-in YouTube Music account: cookies kept privately on the phone, never backed up. */
class YtMusicAccount(context: Context) : PlaylistSource {
    private val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)

    val session: YtMusicSession? get() = prefs.getString(COOKIES, null)?.let(::YtMusicSession)?.takeIf { it.connected }

    fun signIn(cookies: String): Boolean {
        val connected = YtMusicSession(cookies).connected
        if (connected) prefs.edit { putString(COOKIES, cookies) }
        return connected
    }

    fun signOut() = prefs.edit { remove(COOKIES) }

    fun library(): List<PlaylistChoice> = YtMusicParser.library(browse("FEmusic_liked_playlists"))

    override fun tracks(id: PlaylistId): List<Track> = YtMusicParser.tracks(browse("VL${id.value}"))

    private fun browse(browseId: String): String {
        val session = session ?: throw IOException("Non connecté à YouTube Music")
        val body = JSONObject().put("browseId", browseId).put("context", CONTEXT).toString()
        val request = Request.Builder().url("${YtMusicSession.ORIGIN}/youtubei/v1/browse?prettyPrint=false")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Cookie", session.cookies)
            .header("Authorization", requireNotNull(session.authorization(System.currentTimeMillis() / MILLIS)))
            .header("Origin", YtMusicSession.ORIGIN)
            .header("X-Origin", YtMusicSession.ORIGIN)
            .header("X-Goog-AuthUser", "0")
            .header("User-Agent", NewPipeDownloader.USER_AGENT)
            .build()
        return NewPipeDownloader.instance.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("YouTube Music HTTP ${response.code}")
            response.body.string()
        }
    }

    private companion object {
        const val COOKIES = "cookies"
        const val MILLIS = 1000
        val CONTEXT: JSONObject = JSONObject().put(
            "client",
            JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20251110.01.00").put("hl", "fr").put("gl", "FR"),
        )
    }
}
