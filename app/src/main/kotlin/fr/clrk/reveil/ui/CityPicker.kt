package fr.clrk.reveil.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.clrk.reveil.core.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Where the city search goes: [search] and [choose] are blocking and run off the main thread. */
interface CitySource {
    val current: Place?
    fun search(query: String): List<Place>
    fun choose(place: Place)
}

/** "Météo du matin": type a city once; only its name leaves the phone. */
@Composable
fun CityPicker(source: CitySource, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(emptyList<Place>()) }
    var chosen by remember { mutableStateOf<Place?>(null) }
    val leave = rememberLeave()
    val close = { leave.then(onClose) }
    LaunchedEffect(query) {
        delay(TYPING_MS)
        found = withContext(Dispatchers.IO) { source.search(query) }
    }
    LaunchedEffect(chosen) {
        val place = chosen ?: return@LaunchedEffect
        withContext(Dispatchers.IO) { source.choose(place) }
        close()
    }
    Layer(close) {
        Appear(leave) { CityScreen(CityUi(query, found, source.current), { query = it }, { chosen = it }, close) }
    }
}

/** What the city screen shows. */
data class CityUi(val query: String, val found: List<Place>, val current: Place?)

@Composable
fun CityScreen(ui: CityUi, onQuery: (String) -> Unit, onPick: (Place) -> Unit, onClose: () -> Unit) {
    NightPage { CityContent(ui, onQuery, onPick, onClose) }
}

@Composable
private fun CityContent(ui: CityUi, onQuery: (String) -> Unit, onPick: (Place) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Text("✕", Modifier.padding(vertical = 16.dp).clickable(onClick = onClose), color = Night.CREAM, fontSize = 18.sp)
        Text("Météo du matin", Modifier.reveal(0), fontSize = 30.sp, fontWeight = FontWeight.ExtraLight)
        Text(
            "La lumière de la sonnerie suit le temps qu'il fera. Indiquez une ville une fois, rien ne quitte le téléphone à part ce nom.",
            Modifier.padding(top = 8.dp).reveal(1), color = Night.CAPTION, fontSize = 14.sp,
        )
        ui.current?.let {
            Text("Ville actuelle : ${it.name}", Modifier.padding(top = 14.dp).reveal(2), color = Night.EMBER, fontSize = 14.sp)
        }
        OutlinedTextField(
            ui.query, onQuery, Modifier.fillMaxWidth().padding(top = 18.dp).reveal(order = 3),
            singleLine = true, placeholder = { Text("Votre ville") },
        )
        val found = remember(ui.found) { SystemClock.uptimeMillis() }
        LazyColumn(Modifier.padding(top = 6.dp)) {
            itemsIndexed(ui.found, key = { i, place -> "$i ${place.latitude},${place.longitude}" }) { i, place ->
                CityRow(place, place == ui.current, Modifier.reveal(row(i, 0), found)) { onPick(place) }
            }
        }
    }
}

@Composable
private fun CityRow(place: Place, current: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(place.name, fontSize = 16.sp)
            if (place.detail.isNotEmpty()) Text(place.detail, color = Night.CAPTION, fontSize = 12.sp)
        }
        if (current) Text("✓", color = Night.EMBER)
    }
    HorizontalDivider(color = Night.LINE)
}

private const val TYPING_MS = 400L
