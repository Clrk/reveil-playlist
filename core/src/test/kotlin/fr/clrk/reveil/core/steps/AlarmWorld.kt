package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.AlarmRepository
import fr.clrk.reveil.core.AlarmScheduler
import fr.clrk.reveil.core.AlarmService
import fr.clrk.reveil.core.TextStore
import fr.clrk.reveil.core.TriggerKey
import fr.clrk.reveil.core.WakeInfo
import java.time.Instant
import java.time.ZonedDateTime

/** Testing API: drives the alarm use cases with an in-memory phone. */
class AlarmWorld {
    var now: ZonedDateTime = Formats.moment("2026-11-16 06:00")
    val phone = FakePhoneScheduler()
    val disk = MemoryStore()
    var cachedTracks = 0
    var freed: String? = null
    var download: WakeInfo.Download = WakeInfo.Download.Waiting
    var service = newService()

    fun newService() = AlarmService(AlarmRepository(disk), phone) { now }

    private val ids = mutableMapOf<String, String>()

    fun remember(alarm: Alarm) { ids[alarm.label] = alarm.id }

    fun id(label: String): String = ids.getValue(label)

    fun alarm(label: String): Alarm = service.list().single { it.id == id(label) }

    fun planned(label: String, kind: TriggerKey.Kind = TriggerKey.Kind.RING) =
        phone.planned[TriggerKey(id(label), kind)]
}

class FakePhoneScheduler : AlarmScheduler {
    val planned = mutableMapOf<TriggerKey, Instant>()

    override fun schedule(key: TriggerKey, at: Instant) { planned[key] = at }

    override fun cancel(key: TriggerKey) { planned.remove(key) }
}

class MemoryStore : TextStore {
    var content: String? = null

    override fun read() = content

    override fun write(text: String) { content = text }
}
