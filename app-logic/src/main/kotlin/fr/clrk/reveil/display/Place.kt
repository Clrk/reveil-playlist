package fr.clrk.reveil.display

import fr.clrk.reveil.core.TextStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder

/** City whose weather is shown; [detail] tells apart cities with the same name (region, country). */
data class Place(val name: String, val latitude: Double, val longitude: Double, val detail: String = "") {
    internal fun json() = buildJsonObject {
        put("name", name)
        put("latitude", latitude)
        put("longitude", longitude)
        put("detail", detail)
    }

    internal companion object {
        fun of(o: JsonObject) = Place(o.text("name"), o.number("latitude"), o.number("longitude"), o.text("detail"))

        fun found(o: JsonObject) = Place(
            o.text("name"), o.number("latitude"), o.number("longitude"),
            listOf(o.text("admin1"), o.text("country")).filter { it.isNotEmpty() }.joinToString(", "),
        )

        private fun JsonObject.text(key: String) = get(key)?.let { it.jsonPrimitive.content }.orEmpty()

        private fun JsonObject.number(key: String) = getValue(key).jsonPrimitive.double
    }
}

/** Open-Meteo city search (free, no key): URL to call and how to read it. */
object Geocoding {
    private const val COUNT = 8

    fun url(query: String) = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}" +
        "&count=$COUNT&language=fr&format=json"

    /** Cities found in an answer; empty when nothing matched or the answer is unreadable. */
    fun places(body: String): List<Place> = runCatching {
        Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray.orEmpty().map { Place.found(it.jsonObject) }
    }.getOrDefault(emptyList())
}

/** The weather city the user chose; there is none until they pick one. */
class PlaceStore(private val store: TextStore) {
    fun place(): Place? = store.read()?.let { runCatching { Place.of(Json.parseToJsonElement(it).jsonObject) }.getOrNull() }

    fun choose(place: Place) = store.write(place.json().toString())

    /** Open-Meteo forecast to fetch, or null while no city is chosen. */
    fun forecastUrl(): String? = place()?.let(OpenMeteo::url)
}
