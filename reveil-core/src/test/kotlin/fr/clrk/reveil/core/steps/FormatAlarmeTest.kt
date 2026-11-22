package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.AlarmFormat
import fr.clrk.reveil.core.Recurrence
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestFactory
import java.time.LocalTime

/** Testing API: one alarm sent from an app and read back by another. */
class FormatWorld {
    lateinit var alarm: Alarm
    lateinit var message: String
    lateinit var read: AlarmFormat.Decoded

    fun messageWith(format: String, time: String, extra: String = "") {
        val minutes = LocalTime.parse(time).toSecondOfDay() / SECONDS_PER_MINUTE
        message = """{"format":$format,"alarms":[{"id":"a","minuteOfDay":$minutes,"days":[],"label":"",""" +
            """"enabled":true,"snoozeMinutes":10$extra}]}"""
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}

class FormatAlarmeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/format-alarme.feature", ::FormatWorld) {
        step("une alarme à \"(.+)\" en semaine \"(.+)\" avec la playlist \"(.+)\"") { (time, label, playlist) ->
            alarm = Alarm.at(LocalTime.parse(time), "a").copy(days = Recurrence.WEEKDAYS.days, label = label, playlistId = playlist)
        }
        step("on l'envoie") { message = AlarmFormat.encode(listOf(alarm)) }
        step("on l'envoie puis on la relit") { read = AlarmFormat.decode(AlarmFormat.encode(listOf(alarm))) }
        step("on retrouve la même alarme") { assertEquals(AlarmFormat.Decoded.Alarms(listOf(alarm)), read) }
        step("le message porte le format \"(\\d+)\"") { (n) -> assertTrue(message.startsWith("{\"format\":$n,"), message) }
        step("un message au format \"(\\d+)\" avec une alarme à \"(.+)\" et le champ inconnu \"(\\w+)\"") { (f, t, field) ->
            messageWith(f, t, ",\"$field\":{\"couleur\":\"rose\"}")
        }
        step("un message au format \"(\\d+)\" avec une alarme à \"([^\"]+)\"") { (f, t) -> messageWith(f, t) }
        step("le message \"(.+)\"") { (text) -> message = text.replace("\\\"", "\"") }
        step("on le relit") { read = AlarmFormat.decode(message) }
        step("on lit une alarme à \"(.+)\"") { (t) ->
            assertEquals(LocalTime.parse(t), (read as AlarmFormat.Decoded.Alarms).alarms.single().time)
        }
        step("le message est refusé car son format \"(\\d+)\" est plus récent que le format \"(\\d+)\" connu") { (found, known) ->
            assertEquals(AlarmFormat.Decoded.TooNew(found.toInt(), known.toInt()), read)
        }
        step("le message est illisible") { assertEquals(AlarmFormat.Decoded.Unreadable, read) }
    }
}
