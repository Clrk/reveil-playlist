package fr.clrk.reveil.music

import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

/** Testing API: YouTubeHealth with an in-memory host store. */
class HealthWorld : HealthStore {
    override var unreadable = 0
    var thrown: Throwable? = null

    fun start() = YouTubeHealth.init(this)

    fun answer(error: Throwable?) {
        thrown = runCatching { YouTubeHealth.watch { error?.let { throw it } } }.exceptionOrNull()
    }

    val broken get() = YouTubeHealth.broken.value
}

class SanteTest {
    private val illisible = ParsingException("page illisible")

    @TestFactory
    fun scenarios() = FeatureRunner.run("features/sante.feature", ::HealthWorld) {
        step("YouTube fonctionne") { start() }
        step("YouTube est en panne") { unreadable = 2; start() }
        step("l'hôte a gardé \"(\\d+)\" pages illisibles de suite") { (n) -> unreadable = n.toInt() }
        step("l'app démarre") { start() }
        step("YouTube renvoie une page illisible") { answer(illisible) }
        step("YouTube répond normalement") { answer(null) }
        step("une vidéo est retirée") { answer(ContentNotAvailableException("retirée")) }
        step("YouTube demande un captcha") { answer(ReCaptchaException("captcha", "https://www.youtube.com")) }
        step("le réseau est coupé") { answer(IOException("hors ligne")) }
        step("YouTube est signalé en panne") { assertTrue(broken) }
        step("YouTube n'est pas signalé en panne") { assertFalse(broken) }
        step("YouTube fonctionne de nouveau") { assertFalse(broken) }
        step("l'appelant reçoit l'erreur") { assertEquals(illisible, thrown) }
    }
}
