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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajrpachon.chatapp.ui.components.ChatAppSearchField
import com.ajrpachon.chatapp.utils.GiphyKeyManager
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
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
                text = { Text("Stickers") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("GIFs") },
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
    val packs by vm.installedPacks.collectAsState()
    var selectedPackIndex by remember { mutableIntStateOf(0) }
    val safeIndex by remember(packs.size) {
        derivedStateOf { selectedPackIndex.coerceAtMost((packs.size - 1).coerceAtLeast(0)) }
    }
    val currentPackId by remember(packs, safeIndex) {
        derivedStateOf { packs.getOrNull(safeIndex)?.id }
    }
    val stickers by remember(currentPackId) {
        if (currentPackId != null) vm.stickersForPack(currentPackId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = safeIndex,
            edgePadding = 8.dp,
        ) {
            packs.forEachIndexed { i, pack ->
                Tab(
                    selected = safeIndex == i,
                    onClick = { selectedPackIndex = i },
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
                text = { Text("+", fontSize = 20.sp) },
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
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelected(sticker.imageUrl) },
                    )
                }
            }
        }
    }
}

// ── GIF tab ───────────────────────────────────────────────────────────────────

@Composable
private fun GifTab(onSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<GiphyResult>(GiphyResult.Success(emptyList())) }
    var isLoading by remember { mutableStateOf(true) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(query, refresh) {
        if (query.isNotBlank()) delay(400)
        isLoading = true
        result = searchGiphy(query)
        isLoading = false
    }

    if (showKeyDialog) {
        GiphyApiKeyDialog(
            onSave = { key ->
                GiphyKeyManager.setKey(key)
                showKeyDialog = false
                refresh++
            },
            onDismiss = { showKeyDialog = false },
        )
    }

    Column {
        ChatAppSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar GIFs…",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            result is GiphyResult.ApiKeyInvalid -> Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("GIFs no disponibles", color = Color.Gray)
                Text("Configura tu propia clave API de Giphy para activar esta función.", color = Color.Gray)
                Button(onClick = { showKeyDialog = true }) {
                    Text("Configurar clave API")
                }
            }
            result is GiphyResult.NetworkError -> Box(
                Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center
            ) {
                Text("Error de red. Inténtalo de nuevo.", color = Color.Gray)
            }
            else -> {
            val gifs = (result as GiphyResult.Success).gifs
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(gifs) { gif ->
                    val thumbUrl = gif.images.fixedHeightSmall.url
                    val fullUrl = gif.images.original.url
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = "GIF",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.15f))
                            .clickable { onSelected(fullUrl) },
                    )
                }
            }
            }
        }
    }
}

// ── Giphy API key dialog ──────────────────────────────────────────────────────

@Composable
private fun GiphyApiKeyDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf(GiphyKeyManager.getKey() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clave API de Giphy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Obtén una clave gratuita en developers.giphy.com y pégala aquí.")
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
