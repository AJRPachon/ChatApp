package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.GiphyGifBO
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import com.ajrpachon.chatapp.ui.components.ChatAppSearchField
import com.ajrpachon.chatapp.ui.preview.ChatAppPreview
import com.ajrpachon.chatapp.ui.preview.ThemePreviews
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextButton
import com.ajrpachon.chatapp.ui.theme.ChatAppTextSizes
import org.koin.androidx.compose.koinViewModel

// ── Main picker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerGifPicker(
    onStickerSelected: (url: String) -> Unit,
    onGifSelected: (url: String) -> Unit,
    onOpenStore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.6f),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.stickers_tab_stickers)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.stickers_tab_gifs)) },
            )
        }
        when (selectedTab) {
            0 -> StickerTab(onStickerSelected, onOpenStore)
            1 -> GifTab(onGifSelected)
        }
    }
}

// ── Sticker tab (custom image packs) ─────────────────────────────────────────

@Composable
private fun StickerTab(
    onSelected: (String) -> Unit,
    onOpenStore: () -> Unit,
    vm: StickerPackViewModel = koinViewModel(),
) {
    val stickerState by vm.state.collectAsStateWithLifecycle()
    val packs = stickerState.installedPacks
    var selectedPackIndex by remember { mutableIntStateOf(0) }
    val safeIndex by remember(packs.size) {
        derivedStateOf { selectedPackIndex.coerceAtMost((packs.size - 1).coerceAtLeast(0)) }
    }
    val currentPackId by remember(packs, safeIndex) {
        derivedStateOf { packs.getOrNull(safeIndex)?.id }
    }
    val stickers by remember(currentPackId) {
        val packId = currentPackId
        if (packId != null) vm.stickersForPack(packId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    StickerTabContent(
        packs = packs,
        stickers = stickers,
        selectedIndex = safeIndex,
        onSelectPack = { selectedPackIndex = it },
        onSelected = onSelected,
        onOpenStore = onOpenStore,
    )
}

/** Pack tabs and sticker grid, without the ViewModel, so it can be previewed. */
@Composable
internal fun StickerTabContent(
    packs: List<StickerPackBO>,
    stickers: List<StickerBO>,
    selectedIndex: Int,
    onSelectPack: (Int) -> Unit,
    onSelected: (String) -> Unit,
    onOpenStore: () -> Unit,
) {
    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 8.dp,
        ) {
            packs.forEachIndexed { i, pack ->
                Tab(
                    selected = selectedIndex == i,
                    onClick = { onSelectPack(i) },
                ) {
                    AsyncImage(
                        model = pack.coverUrl,
                        contentDescription = pack.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .size(28.dp),
                    )
                }
            }
            Tab(
                selected = false,
                onClick = onOpenStore,
                text = { Text("+", fontSize = ChatAppTextSizes.TabAction) },
            )
        }

        if (packs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Instala packs desde la tienda →",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(stickers, key = { it.id }) { sticker ->
                    AsyncImage(
                        model = sticker.imageUrl,
                        contentDescription = sticker.tags,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelected(sticker.imageUrl) },
                    )
                }
            }
        }
    }
}

// ── GIF tab ───────────────────────────────────────────────────────────────────

@Composable
private fun GifTab(onSelected: (String) -> Unit, vm: GifPickerViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    GifTabContent(state = state, onIntent = vm::onIntent, onSelected = onSelected)
}

/** Search field and GIF grid, without the ViewModel, so it can be previewed. */
@Composable
internal fun GifTabContent(
    state: GifPickerState,
    onIntent: (GifPickerIntent) -> Unit,
    onSelected: (String) -> Unit,
) {
    if (state.showKeyDialog) {
        GiphyApiKeyDialog(
            initialKey = state.savedApiKey,
            onSave = { key -> onIntent(GifPickerIntent.SaveApiKey(key)) },
            onDismiss = { onIntent(GifPickerIntent.DismissKeyDialog) },
        )
    }

    Column {
        ChatAppSearchField(
            value = state.query,
            onValueChange = { onIntent(GifPickerIntent.QueryChanged(it)) },
            placeholder = stringResource(R.string.gif_search_placeholder),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorState == GifPickerError.API_KEY_INVALID -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.gif_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Configura tu propia clave API de Giphy para activar esta función.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChatAppPrimaryButton(
                    text = stringResource(R.string.gif_configure_key),
                    onClick = { onIntent(GifPickerIntent.ShowKeyDialog) },
                )
            }
            state.errorState == GifPickerError.NETWORK_ERROR -> Box(
                Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.gif_network_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.gifs, key = { it.previewUrl }) { gif ->
                    AsyncImage(
                        model = gif.previewUrl,
                        contentDescription = stringResource(R.string.chat_gif_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            .clickable { onSelected(gif.fullUrl) },
                    )
                }
            }
            }
        }
    }
}

// ── Giphy API key dialog ──────────────────────────────────────────────────────

@Composable
private fun GiphyApiKeyDialog(initialKey: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf(initialKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gif_api_key_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.gif_api_key_help))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.gif_api_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            ChatAppTextButton(text = stringResource(R.string.gif_api_key_save), onClick = { onSave(key) }, enabled = key.isNotBlank())
        },
        dismissButton = {
            ChatAppTextButton(text = stringResource(R.string.gif_api_key_cancel), onClick = onDismiss)
        },
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewPacks = listOf(
    StickerPackBO(id = "p1", name = "Animales", coverUrl = "https://preview.invalid/p1.png", isInstalled = true),
    StickerPackBO(id = "p2", name = "Memes", coverUrl = "https://preview.invalid/p2.png", isInstalled = true),
)

private val previewStickers = List(6) {
    StickerBO(id = "s$it", packId = "p1", imageUrl = "https://preview.invalid/s$it.png", tags = "sticker $it")
}

private val previewGifs = List(6) {
    GiphyGifBO(previewUrl = "https://preview.invalid/g$it.gif", fullUrl = "https://preview.invalid/full$it.gif")
}

@ThemePreviews
@Composable
internal fun StickerTabContentPreview() {
    ChatAppPreview {
        StickerTabContent(
            packs = previewPacks,
            stickers = previewStickers,
            selectedIndex = 0,
            onSelectPack = {},
            onSelected = {},
            onOpenStore = {},
        )
    }
}

@ThemePreviews
@Composable
internal fun StickerTabContentEmptyPreview() {
    ChatAppPreview {
        StickerTabContent(
            packs = emptyList(),
            stickers = emptyList(),
            selectedIndex = 0,
            onSelectPack = {},
            onSelected = {},
            onOpenStore = {},
        )
    }
}

@ThemePreviews
@Composable
internal fun GifTabContentPreview() {
    ChatAppPreview {
        GifTabContent(
            state = GifPickerState(query = "gato", gifs = previewGifs, isLoading = false),
            onIntent = {},
            onSelected = {},
        )
    }
}

@ThemePreviews
@Composable
internal fun GifTabContentLoadingPreview() {
    ChatAppPreview {
        GifTabContent(state = GifPickerState(isLoading = true), onIntent = {}, onSelected = {})
    }
}

@ThemePreviews
@Composable
internal fun GifTabContentApiKeyInvalidPreview() {
    ChatAppPreview {
        GifTabContent(
            state = GifPickerState(isLoading = false, errorState = GifPickerError.API_KEY_INVALID),
            onIntent = {},
            onSelected = {},
        )
    }
}

@ThemePreviews
@Composable
internal fun GifTabContentNetworkErrorPreview() {
    ChatAppPreview {
        GifTabContent(
            state = GifPickerState(isLoading = false, errorState = GifPickerError.NETWORK_ERROR),
            onIntent = {},
            onSelected = {},
        )
    }
}
