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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

// ── Main picker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerGifPicker(
    onStickerSelected: (emoji: String) -> Unit,
    onGifSelected: (url: String) -> Unit,
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
            0 -> StickerTab(onStickerSelected)
            1 -> GifTab(onGifSelected)
        }
    }
}

// ── Sticker tab ───────────────────────────────────────────────────────────────

@Composable
private fun StickerTab(onSelected: (String) -> Unit) {
    var selectedPack by remember { mutableIntStateOf(0) }

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedPack,
            edgePadding = 8.dp,
        ) {
            stickerPacks.forEachIndexed { i, pack ->
                Tab(
                    selected = selectedPack == i,
                    onClick = { selectedPack = i },
                    text = { Text(pack.name.take(4), fontSize = 18.sp) },
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(stickerPacks[selectedPack].emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onSelected(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 32.sp)
                }
            }
        }
    }
}

// ── GIF tab ───────────────────────────────────────────────────────────────────

@Composable
private fun GifTab(onSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<GiphyGif>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(400)
        isLoading = true
        gifs = searchGiphy(query)
        isLoading = false
    }

    Column {
        ChatAppSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar GIFs…",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
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
