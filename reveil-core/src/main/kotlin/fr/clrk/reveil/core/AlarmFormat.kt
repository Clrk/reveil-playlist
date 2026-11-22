package fr.clrk.reveil.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Alarms as they travel between two apps of different versions (the Bonjour and its companion):
 * `{"format":1,"alarms":[…]}`. Unknown fields are ignored; a newer format is refused, never half read.
 * Any change to what is written raises [VERSION] (see AlarmFormatContractTest).
 */
object AlarmFormat {
    const val VERSION = 1

    sealed interface Decoded {
        data class Alarms(val alarms: List<Alarm>) : Decoded

        /** Written by a newer app: update this one before reading it. */
        data class TooNew(val format: Int, val known: Int) : Decoded

        data object Unreadable : Decoded
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun encode(alarms: List<Alarm>): String = json.encodeToString(Envelope(VERSION, alarms.map { AlarmRecord.of(it) }))

    fun decode(text: String): Decoded = try {
        read(text)
    } catch (_: SerializationException) {
        Decoded.Unreadable
    } catch (_: IllegalArgumentException) {
        Decoded.Unreadable
    }

    private fun read(text: String): Decoded {
        val format = json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.intOrNull
        return when {
            format == null -> Decoded.Unreadable
            format > VERSION -> Decoded.TooNew(format, VERSION)
            else -> Decoded.Alarms(json.decodeFromString<Envelope>(text).alarms.map { it.toAlarm() })
        }
    }

    @Serializable
    private data class Envelope(val format: Int, val alarms: List<AlarmRecord>)
}
