package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.SilentUpdate
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.time.ZonedDateTime

/** Testing API: the clock, what will ring next, and whether something rings now. */
class SilencieuseWorld {
    lateinit var now: ZonedDateTime
    var upcoming = listOf<Instant>()
    var ringing = false

    fun allowed() = SilentUpdate.allowed(now.toInstant(), upcoming, ringing)
}

class SilencieuseTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/mise-a-jour-silencieuse.feature", ::SilencieuseWorld) {
        step("nous sommes le \"([^\"]+)\"") { (moment) -> now = Formats.moment(moment) }
        step("un réveil sonnera le \"([^\"]+)\"") { (moment) -> upcoming = upcoming + Formats.moment(moment).toInstant() }
        step("un snooze se termine le \"([^\"]+)\"") { (moment) -> upcoming = upcoming + Formats.moment(moment).toInstant() }
        step("un réveil sonne en ce moment") { ringing = true }
        step("la mise à jour peut s'installer seule") { assertTrue(allowed()) }
        step("la mise à jour attend") { assertFalse(allowed()) }
    }
}
