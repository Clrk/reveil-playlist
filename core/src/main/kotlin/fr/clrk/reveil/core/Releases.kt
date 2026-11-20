package fr.clrk.reveil.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A newer version of the app, published on GitHub, with the APK to install. */
data class Update(val version: String, val apk: String)

/** The app updates itself from its GitHub releases, since it is not on the Play Store. */
object Releases {
    const val LATEST = "https://api.github.com/repos/Clrk/reveil-playlist/releases/latest"

    /** The release in GitHub's [body], when it is newer than the [installed] version and has an APK. */
    fun newer(installed: String, body: String): Update? =
        runCatching { release(Json.parseToJsonElement(body).jsonObject) }.getOrNull()
            ?.takeIf { compare(it.version, installed) > 0 }

    private fun release(json: JsonObject): Update? {
        val version = json.getValue("tag_name").jsonPrimitive.content.removePrefix("v")
        val apk = json.getValue("assets").jsonArray.map { it.jsonObject }
            .firstOrNull { it.getValue("name").jsonPrimitive.content.endsWith(".apk") }
            ?: return null
        return Update(version, apk.getValue("browser_download_url").jsonPrimitive.content)
    }

    /** Compares versions number by number: 0.10 comes after 0.9, and 0.9 is 0.9.0. */
    private fun compare(a: String, b: String): Int {
        val (x, y) = numbers(a) to numbers(b)
        val length = maxOf(x.size, y.size)
        return pad(x, length).zip(pad(y, length)) { m, n -> m.compareTo(n) }.firstOrNull { it != 0 } ?: 0
    }

    private fun numbers(version: String) = DIGITS.findAll(version).map { it.value.toInt() }.toList()

    private fun pad(numbers: List<Int>, length: Int) = numbers + List(length - numbers.size) { 0 }

    private val DIGITS = Regex("\\d+")
}
