package fr.clrk.reveil.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import fr.clrk.reveil.music.YtMusicAccount
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.aspectRatio
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

/** Full-screen picker: my YouTube Music playlists, or a search (a pasted link works too). */
@Composable
fun PlaylistPicker(onPick: (PlaylistChoice) -> Unit, onClose: () -> Unit) {
    val account = YtMusicAccount(LocalContext.current)
    var ui by remember { mutableStateOf(PickerUi(library = if (account.session == null) LibraryUi.NotConnected else LibraryUi.Loading)) }
    val login = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (account.session != null) ui = ui.copy(library = LibraryUi.Loading)
    }
    LaunchedEffect(ui.library) { if (ui.library == LibraryUi.Loading) ui = ui.copy(library = loadLibrary(account)) }
    LaunchedEffect(ui.query) {
        delay(DEBOUNCE_MS)
        ui = ui.copy(loading = true)
        ui = ui.copy(result = withContext(Dispatchers.IO) { search(ui.query) }, loading = false)
    }
    val context = LocalContext.current
    val events = object : PickerEvents {
        override fun query(text: String) { ui = ui.copy(query = text) }
        override fun tab(tab: PickerTab) { ui = ui.copy(tab = tab) }
        override fun pick(choice: PlaylistChoice) = onPick(choice)
        override fun signIn() = login.launch(Intent(context, LoginActivity::class.java))
        override fun signOut() { account.signOut(); ui = ui.copy(library = LibraryUi.NotConnected) }
        override fun close() = onClose()
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Appear { PlaylistPickerScreen(ui, events) }
    }
}

private suspend fun loadLibrary(account: YtMusicAccount): LibraryUi = withContext(Dispatchers.IO) {
    runCatching { account.library() }.fold(
        { LibraryUi.Loaded(it) },
        { LibraryUi.Failed("Impossible de lire votre bibliothèque YouTube Music. Reconnectez-vous ou réessayez.") },
    )
}

/** Searches YouTube Music; a pasted link gets its real name and size. */
private fun search(query: String): SearchResult {
    val result = PlaylistFinder(NewPipePlaylistSearch).find(query)
    val pasted = (result as? SearchResult.Found)?.playlists?.singleOrNull()?.takeIf { it.trackCount < 0 } ?: return result
    return SearchResult.Found(listOf(runCatching { NewPipePlaylistSearch.describe(pasted) }.getOrDefault(pasted)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerScreen(ui: PickerUi, events: PickerEvents) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quelle playlist demain ?") },
                navigationIcon = { IconButton(onClick = events::close) { Icon(Icons.Default.ArrowBack, "Retour") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = ui.tab.ordinal) {
                PickerTab.entries.forEach { tab -> Tab(ui.tab == tab, { events.tab(tab) }, text = { Text(tab.label) }) }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Turn(ui.tab, { from, to -> to > from }) { tab ->
                    Column { if (tab == PickerTab.MINE) Library(ui.library, events) else SearchTab(ui, events) }
                }
            }
        }
    }
}

@Composable
private fun SearchTab(ui: PickerUi, events: PickerEvents) {
    OutlinedTextField(
        ui.query, events::query, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        placeholder = { Text("Artiste, ambiance… ou lien de playlist") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
    if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    Results(ui.result, events::pick)
}

@Composable
private fun Library(library: LibraryUi, events: PickerEvents) {
    when (library) {
        LibraryUi.NotConnected -> SignInCard(events)
        LibraryUi.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
        is LibraryUi.Failed -> Column {
            Hint(library.message)
            OutlinedButton(onClick = events::signIn) { Text("Se reconnecter") }
        }
        is LibraryUi.Loaded -> Crate(library.playlists, events)
    }
}

/** Your playlists as a crate of sleeves, two per row. */
@Composable
private fun Crate(playlists: List<PlaylistChoice>, events: PickerEvents) {
    LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(playlists, key = { it.id.value }) { choice ->
            Column(Modifier.clickable { events.pick(choice) }) {
                Cover(choice.thumbnail, Modifier.fillMaxWidth().aspectRatio(1f))
                Text(choice.title, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle(choice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(span = { GridItemSpan(2) }) { TextButton(onClick = events::signOut) { Text("Se déconnecter de YouTube Music") } }
    }
}

@Composable
private fun SignInCard(events: PickerEvents) {
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Retrouvez vos playlists", style = MaterialTheme.typography.titleMedium)
            Text(
                "Connectez-vous une fois à YouTube Music pour voir vos playlists, " +
                    "même privées, et vos titres « J'aime ». Pas besoin d'abonnement.",
            )
            Button(onClick = events::signIn) { Text("Se connecter à YouTube Music") }
            Text(
                "La session reste sur ce téléphone.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Results(result: SearchResult, onPick: (PlaylistChoice) -> Unit) {
    when (result) {
        SearchResult.Idle -> Hint("Cherchez une playlist YouTube Music par nom d'artiste, de genre ou d'ambiance.")
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
        Cover(choice.thumbnail, Modifier.size(56.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(choice.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle(choice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun subtitle(choice: PlaylistChoice) =
    listOfNotNull(choice.author.ifBlank { null }, choice.trackCount.takeIf { it >= 0 }?.let { "$it titres" }).joinToString(" · ")
