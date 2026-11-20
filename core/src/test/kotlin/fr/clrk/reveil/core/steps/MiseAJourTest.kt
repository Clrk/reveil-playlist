package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Releases
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

    fun update() = Releases.newer(installed, answer)
}

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
    }
}
