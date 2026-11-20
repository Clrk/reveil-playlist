package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.TriggerKey
import fr.clrk.reveil.core.bdd.FeatureRunner
import fr.clrk.reveil.core.bdd.StepRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

private const val ALARM = "l'alarme \"([^\"]*)\""
private const val PONCTUAL = "l'alarme ponctuelle \"([^\"]+)\" à \"([^\"]+)\""
private const val REPEATED = "$ALARM à \"([^\"]+)\" répétée \"([^\"]+)\""

fun StepRegistry<AlarmWorld>.alarmSteps() {
    creationSteps()
    settingsSteps()
    lifecycleSteps()
    snoozeSteps()
    phoneSteps()
    assertionSteps()
}

private fun StepRegistry<AlarmWorld>.creationSteps() {
    step("nous sommes le \"([^\"]+)\"") { (moment) -> now = Formats.moment(moment) }
    step("(?:je crée )?$REPEATED") { (label, time, days) ->
        remember(service.create(Formats.time(time), Formats.days(days), label))
    }
    step("$REPEATED avec la playlist \"([^\"]+)\" nommée \"([^\"]+)\"") { values ->
        val (label, time, days, playlist) = values
        val created = service.create(Formats.time(time), Formats.days(days), label, playlistId = playlist)
        remember(service.update(created.copy(playlistTitle = values[4])))
    }
    step("$REPEATED avec la playlist \"([^\"]+)\" nommée \"([^\"]+)\" et la pochette \"([^\"]+)\"") { values ->
        val created = service.create(Formats.time(values[1]), Formats.days(values[2]), values[0], playlistId = values[3])
        remember(service.update(created.copy(playlistTitle = values[4], playlistCover = values[5])))
    }
    step("$REPEATED avec un snooze de \"(\\d+)\" minutes") { (label, time, days, minutes) ->
        remember(service.create(Formats.time(time), Formats.days(days), label, snoozeMinutes = minutes.toInt()))
    }
    step(PONCTUAL) { (label, time) -> remember(service.create(Formats.time(time), emptySet(), label)) }
}

private fun StepRegistry<AlarmWorld>.settingsSteps() {
    step("$REPEATED avec \"(\\d+)\" snoozes au maximum") { (label, time, days, max) ->
        remember(service.update(created(label, time, days).copy(maxSnoozes = max.toInt())))
    }
    step("$REPEATED sans limite de snoozes") { (label, time, days) ->
        remember(service.update(created(label, time, days).copy(maxSnoozes = null)))
    }
    step("$REPEATED avec \"(\\d+)\" snoozes au maximum et une montée de \"(\\d+)\" secondes") { values ->
        val alarm = created(values[0], values[1], values[2])
        remember(service.update(alarm.copy(maxSnoozes = values[3].toInt(), rampSeconds = values[4].toInt())))
    }
    step("$ALARM enregistrée par la version 0.3.0") { (label) ->
        disk.content = OLD_RECORD.replace("LABEL", label)
        remember(service.list().single { it.label == label })
    }
}

private const val OLD_RECORD = """[{"id":"ancienne","minuteOfDay":450,"days":[1],"label":"LABEL","enabled":true,"snoozeMinutes":10}]"""

private fun AlarmWorld.created(label: String, time: String, days: String) =
    service.create(Formats.time(time), Formats.days(days), label)

private fun StepRegistry<AlarmWorld>.lifecycleSteps() {
    step("je désactive $ALARM") { (label) -> service.setEnabled(alarm(label).id, false) }
    step("je réactive $ALARM") { (label) -> service.setEnabled(alarm(label).id, true) }
    step("je change l'heure de $ALARM à \"([^\"]+)\"") { (label, time) ->
        service.update(alarm(label).copy(time = Formats.time(time)))
    }
    step("je supprime $ALARM") { (label) -> freed = service.delete(alarm(label).id) }
    step("$ALARM sonne le \"([^\"]+)\"") { (label, moment) ->
        now = Formats.moment(moment)
        service.onRing(alarm(label).id)
    }
    step("j'arrête $ALARM") { (label) -> service.dismiss(alarm(label).id) }
    step("l'alarme oubliée \"([^\"]+)\" sonne, est repoussée, arrêtée puis réactivée") { (label) ->
        service.onRing(id(label))
        service.snooze(id(label))
        service.dismiss(id(label))
        service.setEnabled(id(label), true)
    }
}

private fun StepRegistry<AlarmWorld>.snoozeSteps() {
    step("je repousse $ALARM à \"([^\"]+)\"") { (label, time) ->
        now = now.with(Formats.time(time))
        service.snooze(alarm(label).id)
    }
    step("le snooze de $ALARM sonne le \"([^\"]+)\"") { (label, moment) ->
        now = Formats.moment(moment)
        service.onRing(alarm(label).id, TriggerKey.Kind.SNOOZE)
    }
    step("je ne peux plus repousser $ALARM à \"([^\"]+)\"") { (label, time) ->
        now = now.with(Formats.time(time))
        assertFalse(service.snooze(alarm(label).id))
    }
}

private fun StepRegistry<AlarmWorld>.phoneSteps() {
    step("le téléphone a oublié toutes les programmations") { phone.planned.clear() }
    step("le téléphone redémarre") { service.rescheduleAll() }
    step("le téléphone redémarre le \"([^\"]+)\"") { (moment) ->
        now = Formats.moment(moment)
        service.rescheduleAll()
    }
    step("l'application redémarre") { service = newService() }
    step("les données enregistrées sont corrompues") { disk.content = "{pas du json" }
}

private fun StepRegistry<AlarmWorld>.assertionSteps() {
    step("$ALARM est programmée pour le \"([^\"]+)\"") { (label, moment) ->
        assertEquals(moment, Formats.show(planned(label)?.atZone(Formats.zone)))
    }
    step("$ALARM n'est pas programmée") { (label) ->
        assertEquals(emptyList<TriggerKey>(), phone.planned.keys.filter { it.alarmId == id(label) })
    }
    step("$ALARM est désactivée") { (label) -> assertFalse(alarm(label).enabled) }
    step("la liste des alarmes contient \"([^\"]+)\"") { (label) -> alarm(label) }
    step("la liste des alarmes est \"([^\"]*)\"") { (labels) ->
        assertEquals(Formats.list(labels), service.list().map { it.label })
    }
    step("$ALARM sonne à \"([^\"]+)\" les \"([^\"]+)\" avec la playlist \"([^\"]+)\" nommée \"([^\"]+)\"") { values ->
        val alarm = alarm(values[0])
        val expected = listOf(Formats.time(values[1]), Formats.days(values[2]), values[3], values[4])
        assertEquals(expected, listOf(alarm.time, alarm.days, alarm.playlistId, alarm.playlistTitle))
    }
    step("le prochain réveil est \"([^\"]+)\" le \"([^\"]+)\"") { (label, moment) ->
        val next = service.next()
        assertEquals(label to moment, next?.let { it.first.label to Formats.show(it.second) })
    }
    step("il n'y a pas de prochain réveil") { assertNull(service.next()) }
    snoozeAssertions()
}

private fun StepRegistry<AlarmWorld>.snoozeAssertions() {
    step("$ALARM sonnera à nouveau le \"([^\"]+)\"") { (label, moment) ->
        assertEquals(moment, Formats.show(planned(label, TriggerKey.Kind.SNOOZE)?.atZone(Formats.zone)))
    }
    step("il reste \"(\\d+)\" snoozes à $ALARM") { (count, label) ->
        assertEquals(count.toInt(), alarm(label).snoozesLeft)
    }
    step("$ALARM n'a pas de limite de snoozes") { (label) -> assertNull(alarm(label).snoozesLeft) }
    step("le volume de $ALARM monte sur \"(\\d+)\" secondes") { (label, seconds) ->
        assertEquals(seconds.toInt(), alarm(label).rampSeconds)
    }
    step("la pochette de $ALARM est \"([^\"]+)\"") { (label, cover) -> assertEquals(cover, alarm(label).playlistCover) }
    step("les chansons de la playlist \"([^\"]+)\" sont effacées du téléphone") { (playlist) -> assertEquals(playlist, freed) }
    step("aucune chanson n'est effacée du téléphone") { assertNull(freed) }
    step("$ALARM n'a pas de snooze en attente") { (label) ->
        assertNull(phone.planned[TriggerKey(id(label), TriggerKey.Kind.SNOOZE)])
    }
}

class GestionTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/gestion.feature", ::AlarmWorld) { alarmSteps() }
}

class SnoozeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/snooze.feature", ::AlarmWorld) { alarmSteps() }
}
