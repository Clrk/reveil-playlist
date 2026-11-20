package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Geocoding
import fr.clrk.reveil.core.Place
import fr.clrk.reveil.core.PlaceStore
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

/** Testing API: a weather city kept in memory, and the answers Open-Meteo's geocoding would give. */
class VilleWorld {
    val disk = MemoryStore()
    val places get() = PlaceStore(disk)
    val found = mutableListOf<String>()
    var answer: String? = null
    var url = ""

    fun find(name: String, region: String?, country: String, lat: String, lon: String) {
        val admin = region?.let { ""","admin1":"$it"""" }.orEmpty()
        found += """{"name":"$name"$admin,"country":"$country","latitude":$lat,"longitude":$lon}"""
    }

    fun results() = Geocoding.places(answer ?: """{"results":[${found.joinToString(",")}]}""")
}

class VilleTest {
    private val num = "\"(-?[\\d.]+)\""

    @TestFactory
    fun scenarios() = FeatureRunner.run("features/ville.feature", ::VilleWorld) {
        step("aucune ville choisie") { disk.content = null }
        step("la ville enregistrée est illisible") { disk.content = "{pas du json" }
        step("je choisis la ville \"([^\"]+)\" à $num et $num") { (name, lat, lon) ->
            places.choose(Place(name, lat.toDouble(), lon.toDouble()))
        }
        step("je cherche la ville \"([^\"]+)\"") { (query) -> url = Geocoding.url(query) }
        step("Open-Meteo trouve \"([^\"]+)\" en \"([^\"]+)\", \"([^\"]+)\" à $num et $num") { v -> find(v[0], v[1], v[2], v[3], v[4]) }
        step("Open-Meteo trouve \"([^\"]+)\" sans région, en \"([^\"]+)\" à $num et $num") { v -> find(v[0], null, v[1], v[2], v[3]) }
        step("Open-Meteo répond \"([^\"]+)\"") { (body) -> answer = body }
        step("aucune prévision n'est demandée") { assertNull(places.forecastUrl()) }
        step("la ville retenue est \"([^\"]+)\"") { (name) -> assertEquals(name, places.place()?.name) }
        step("la prévision est demandée pour $num et $num") { (lat, lon) ->
            assertEquals(true, places.forecastUrl()?.contains("latitude=$lat&longitude=$lon"))
        }
        step("l'adresse appelée est \"([^\"]+)\"") { (expected) -> assertEquals(expected, url) }
        step("la ville (\\d+) est \"([^\"]+)\" avec \"([^\"]+)\"") { (i, name, detail) ->
            assertEquals(name to detail, results()[i.toInt() - 1].let { it.name to it.detail })
        }
        step("aucune ville n'est trouvée") { assertEquals(emptyList<Place>(), results()) }
    }
}
