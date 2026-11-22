package fr.clrk.reveil.display.steps

import fr.clrk.reveil.display.Download
import fr.clrk.reveil.display.Releases
import fr.clrk.reveil.display.UpdateBanner
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

/** Testing API: the installed version, and what GitHub's latest release answer would be. */
class MiseAJourWorld {
    var installed = ""
    var answer = ""

    fun publish(tag: String, apk: String?) {
        val assets = apk?.let { """[{"name":"notes.txt","browser_download_url":"https://example.org/notes.txt"},""" +
            """{"name":"$it","browser_download_url":"https://example.org/$it"}]""" } ?: "[]"
        answer = """{"tag_name":"$tag","assets":$assets}"""
    }

    var version = ""
    var download: Download? = null

    fun update() = Releases.newer(installed, answer)

    fun banner() = UpdateBanner.of(version, download)
}

private const val MB = 1_000_000L

class MiseAJourTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/mise-a-jour.feature", ::MiseAJourWorld) {
        step("la version installée \"([^\"]+)\"") { (v) -> installed = v }
        step("GitHub publie \"([^\"]+)\" avec l'APK \"([^\"]+)\"") { (tag, apk) -> publish(tag, apk) }
        step("GitHub publie \"([^\"]+)\" sans APK") { (tag) -> publish(tag, null) }
        step("GitHub répond \"([^\"]+)\"") { (body) -> answer = body }
        step("l'adresse de vérification est \"([^\"]+)\"") { (url) -> assertEquals(url, Releases.LATEST) }
        step("la mise à jour proposée est \"([^\"]+)\"") { (v) -> assertEquals(v, update()?.version) }
        step("elle se télécharge depuis \"([^\"]+)\"") { (url) -> assertEquals(url, update()?.apk) }
        step("aucune mise à jour n'est proposée") { assertNull(update()) }
        step("la nouvelle version \"([^\"]+)\"") { (v) -> version = v }
        step("(\\d+) Mo sur (\\d+) Mo sont téléchargés") { (done, total) ->
            download = Download.Running(done.toLong() * MB, total.toLong() * MB)
        }
        step("(\\d+) Mo d'une taille inconnue sont téléchargés") { (done) -> download = Download.Running(done.toLong() * MB, -1) }
        step("le téléchargement est terminé") { download = Download.Done }
        step("le téléchargement échoue") { download = Download.Failed }
        step("le bandeau dit \"([^\"]+)\" avec le bouton \"([^\"]+)\"") { (text, button) ->
            assertEquals(text to button, banner().text to banner().button)
        }
        step("le bandeau dit \"([^\"]+)\" sans bouton") { (text) -> assertEquals(text to null, banner().text to banner().button) }
        step("le fichier de la version \"([^\"]+)\" s'appelle \"([^\"]+)\"") { (v, name) ->
            assertEquals(name, UpdateBanner.fileName(v))
        }
    }
}
