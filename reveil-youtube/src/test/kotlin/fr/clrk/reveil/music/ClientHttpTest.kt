package fr.clrk.reveil.music

import fr.clrk.reveil.core.bdd.FeatureRunner
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/** Testing API: a host client that answers without network and remembers what went through it. */
class HostWorld {
    val seen = mutableListOf<okhttp3.Request>()
    var code = 200
    var error: Throwable? = null

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        seen += chain.request()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("")
            .body("{}".toResponseBody()).build()
    }.build()

    fun init() = YouTube.init(client)

    fun ask(headers: Map<String, List<String>> = emptyMap()) {
        error = runCatching { NewPipe.getDownloader().get(URL, headers) }.exceptionOrNull()
    }

    fun header(name: String) = seen.single().header(name)

    companion object {
        const val URL = "https://www.youtube.com/results?search_query=lofi"
    }
}

class ClientHttpTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/client-http.feature", ::HostWorld) {
        step("un hôte qui fournit son propre client HTTP") { init() }
        step("YouTube répond \"(\\d+)\"") { (status) -> code = status.toInt() }
        step("YouTube est interrogé") { ask() }
        step("YouTube est interrogé avec l'en-tête \"(.+)\" à \"(.+)\"") { (name, value) -> ask(mapOf(name to listOf(value))) }
        step("la requête est passée par le client de l'hôte") { assertEquals(HostWorld.URL, seen.single().url.toString()) }
        step("elle se présente comme un navigateur") { assertEquals(NewPipeDownloader.USER_AGENT, header("User-Agent")) }
        step("la requête porte l'en-tête \"(.+)\" à \"(.+)\"") { (name, value) -> assertEquals(value, header(name)) }
        step("l'extracteur reçoit une demande de captcha") { assertTrue(error is ReCaptchaException, "error=$error") }
    }
}
