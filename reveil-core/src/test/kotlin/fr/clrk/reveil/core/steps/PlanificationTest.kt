package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.NextTrigger
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory
import java.time.Duration
import java.time.ZonedDateTime

class PlanificationWorld {
    lateinit var now: ZonedDateTime
    var next: ZonedDateTime? = null

    fun set(alarm: Alarm) { next = NextTrigger.after(alarm, now) }
}

class PlanificationTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/planification.feature", ::PlanificationWorld) {
        step("nous sommes le \"(.+)\"") { (moment) -> now = Formats.moment(moment) }
        step("je règle une alarme ponctuelle à \"(.+)\"") { (time) -> set(Alarm.at(Formats.time(time))) }
        step("je règle une alarme à \"(.+)\" répétée \"(.+)\"") { (time, days) ->
            set(Alarm.at(Formats.time(time)).copy(days = Formats.days(days)))
        }
        step("je règle une alarme désactivée à \"(.+)\"") { (time) ->
            set(Alarm.at(Formats.time(time)).copy(enabled = false))
        }
        step("elle sonnera le \"(.+)\"") { (moment) -> assertEquals(moment, Formats.show(next)) }
        step("elle ne sonnera pas") { assertEquals(null, next) }
        step("ce sera dans \"(\\d+)\" heures") { (hours) ->
            assertEquals(Duration.ofHours(hours.toLong()), Duration.between(now, next))
        }
    }
}
