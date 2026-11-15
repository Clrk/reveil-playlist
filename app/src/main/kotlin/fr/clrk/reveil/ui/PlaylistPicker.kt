package fr.clrk.reveil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import fr.clrk.reveil.core.PlaylistChoice
import fr.clrk.reveil.core.PlaylistFinder
import fr.clrk.reveil.core.SearchResult
import fr.clrk.reveil.music.NewPipePlaylistSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val DEBOUNCE_MS = 500L

/** Full-screen search: type a name (or paste a link), tap a playlist. */
@Composable
fun PlaylistPicker(onPick: (PlaylistChoice) -> Unit, onClose: () -> Unit) {
    var ui by remember { mutableStateOf(PickerUi()) }
    LaunchedEffect(ui.query) {
        delay(DEBOUNCE_MS)
        ui = ui.copy(loading = true)
        ui = ui.copy(result = withContext(Dispatchers.IO) { search(ui.query) }, loading = false)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PlaylistPickerScreen(ui, { ui = ui.copy(query = it) }, onPick, onClose)
    }
}

/** Searches YouTube Music; a pasted link gets its real name and size. */
private fun search(query: String): SearchResult {
    val result = PlaylistFinder(NewPipePlaylistSearch).find(query)
    val pasted = (result as? SearchResult.Found)?.playlists?.singleOrNull()?.takeIf { it.trackCount < 0 } ?: return result
    return SearchResult.Found(listOf(runCatching { NewPipePlaylistSearch.describe(pasted) }.getOrDefault(pasted)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerScreen(ui: PickerUi, onQuery: (String) -> Unit, onPick: (PlaylistChoice) -> Unit, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choisir une playlist") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Retour") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                ui.query, onQuery, Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Artiste, ambiance… ou lien de playlist") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Results(ui.result, onPick)
        }
    }
}

@Composable
private fun Results(result: SearchResult, onPick: (PlaylistChoice) -> Unit) {
    when (result) {
        SearchResult.Idle -> Hint("Cherche une playlist YouTube Music par nom d'artiste, de genre ou d'ambiance.")
        is SearchResult.Message -> Hint(result.text)
        is SearchResult.Found -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(result.playlists, key = { it.id.value }) { PlaylistRow(it, Modifier.clickable { onPick(it) }) }
        }
    }
}

@Composable
private fun Hint(text: String) = Text(text, Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

/** One playlist: cover, title, author and size. Also used in the alarm editor. */
@Composable
fun PlaylistRow(choice: PlaylistChoice, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(choice.thumbnail)
        Column(Modifier.padding(start = 12.dp)) {
            Text(choice.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle(choice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Cover(url: String?) {
    val shape = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer)
    Box(shape, contentAlignment = Alignment.Center) {
        Text("♪", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        url?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

private fun subtitle(choice: PlaylistChoice) =
    listOfNotNull(choice.author.ifBlank { null }, choice.trackCount.takeIf { it >= 0 }?.let { "$it titres" }).joinToString(" · ")
