package com.ajrpachon.chatapp.ui.usagestats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun UsageStatsScreen(onBack: () -> Unit) {
    val vm: UsageStatsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ChatAppTopBar(title = "Estadísticas de uso", onBack = onBack) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SectionTitle("Mensajes")
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ChatBubble,
                        label = "Enviados",
                        value = state.totalMessagesSent.toString(),
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ChatBubble,
                        label = "Recibidos",
                        value = state.totalMessagesReceived.toString(),
                    )
                }
            }

            item {
                SectionTitle("Llamadas")
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Call,
                        label = "Total llamadas",
                        value = state.totalCalls.toString(),
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        label = "Minutos",
                        value = state.totalCallMinutes.toString(),
                    )
                }
            }

            item {
                SectionTitle("Archivos multimedia")
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Image,
                        label = "Imágenes",
                        value = state.totalImages.toString(),
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AudioFile,
                        label = "Audios",
                        value = state.totalAudio.toString(),
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VideoFile,
                        label = "Videos",
                        value = state.totalVideos.toString(),
                    )
                }
            }

            if (state.mostActiveConvName.isNotBlank()) {
                item {
                    SectionTitle("Conversación más activa")
                }
                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Star,
                        label = "Más activa",
                        value = state.mostActiveConvName,
                    )
                }
            }

            item {
                SectionTitle("Mensajes últimos 7 días")
            }
            item {
                MessagesBarChart(
                    data = state.messagesPerDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessagesBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val maxCount = data.maxOf { it.second }.coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val barCount = data.size
            val spacing = size.width * 0.06f
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (size.width - totalSpacing) / barCount
            val chartHeight = size.height

            data.forEachIndexed { index, (_, count) ->
                val barHeight = (count.toFloat() / maxCount) * chartHeight * 0.85f
                val x = spacing + index * (barWidth + spacing)
                val y = chartHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }

        // Day labels below bars
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
