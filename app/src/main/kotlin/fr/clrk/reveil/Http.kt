package fr.clrk.reveil

import fr.clrk.reveil.music.NewPipeDownloader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** The app's one HTTP client: YouTube, weather and updates all go through it. */
object Http {
    private const val TIMEOUT_SECONDS = 20L

    val client: OkHttpClient = OkHttpClient.Builder().readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).build()

    /** Downloads audio from YouTube's servers, presenting itself as the browser NewPipeExtractor talks as. */
    val audio: OkHttpClient = client.newBuilder()
        .addInterceptor { it.proceed(it.request().newBuilder().header("User-Agent", NewPipeDownloader.USER_AGENT).build()) }
        .build()
}

/** Blocking GET: the body when the server answers 2xx, null on any failure. */
fun OkHttpClient.text(url: String, headers: Map<String, String> = emptyMap()): String? = runCatching {
    val request = Request.Builder().url(url).apply { headers.forEach { (name, value) -> header(name, value) } }.build()
    newCall(request).execute().use { if (it.isSuccessful) it.body.string() else null }
}.getOrNull()
