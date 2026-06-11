package com.ajrpachon.chatapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer brush ─────────────────────────────────────────────────────────────

@Composable
fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface
    val shimmerColors = listOf(
        base.copy(alpha = 0.08f),
        base.copy(alpha = 0.18f),
        base.copy(alpha = 0.08f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 600f, translateAnim - 600f),
        end = Offset(translateAnim, translateAnim),
    )
}

// ── Primitive shimmer blocks ──────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush()),
    )
}

@Composable
fun ShimmerCircle(size: Dp) {
    ShimmerBox(modifier = Modifier.size(size), shape = CircleShape)
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    ShimmerBox(modifier = modifier.height(height), shape = RoundedCornerShape(6.dp))
}

// ── Conversation list skeleton ────────────────────────────────────────────────

@Composable
fun ConversationListSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(9) {
            ConversationRowSkeleton()
        }
    }
}

@Composable
private fun ConversationRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerCircle(size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerLine(modifier = Modifier.fillMaxWidth(0.55f), height = 13.dp)
            Spacer(Modifier.height(6.dp))
            ShimmerLine(modifier = Modifier.fillMaxWidth(0.80f), height = 11.dp)
        }
    }
}

// ── Chat messages skeleton ────────────────────────────────────────────────────

private data class SkeletonBubble(val fromMe: Boolean, val widthFraction: Float)

private val chatSkeletonPattern = listOf(
    SkeletonBubble(fromMe = false, widthFraction = 0.60f),
    SkeletonBubble(fromMe = false, widthFraction = 0.45f),
    SkeletonBubble(fromMe = true,  widthFraction = 0.50f),
    SkeletonBubble(fromMe = true,  widthFraction = 0.35f),
    SkeletonBubble(fromMe = false, widthFraction = 0.65f),
    SkeletonBubble(fromMe = true,  widthFraction = 0.55f),
    SkeletonBubble(fromMe = false, widthFraction = 0.40f),
    SkeletonBubble(fromMe = true,  widthFraction = 0.70f),
    SkeletonBubble(fromMe = false, widthFraction = 0.50f),
    SkeletonBubble(fromMe = true,  widthFraction = 0.38f),
)

@Composable
fun ChatMessagesSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        userScrollEnabled = false,
        contentPadding = contentPadding,
    ) {
        items(chatSkeletonPattern.size) { index ->
            val bubble = chatSkeletonPattern[index]
            ChatBubbleSkeleton(fromMe = bubble.fromMe, widthFraction = bubble.widthFraction)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChatBubbleSkeleton(fromMe: Boolean, widthFraction: Float) {
    val shape = if (fromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start,
    ) {
        ShimmerBox(
            modifier = Modifier
                .widthIn(min = 60.dp)
                .fillMaxWidth(widthFraction)
                .height(38.dp),
            shape = shape,
        )
    }
}

// ── Invitations skeleton ──────────────────────────────────────────────────────

@Composable
fun InvitationsSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(4) {
            InvitationItemSkeleton()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InvitationItemSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ShimmerLine(modifier = Modifier.fillMaxWidth(0.45f), height = 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerLine(modifier = Modifier.fillMaxWidth(0.30f), height = 11.dp)
        Spacer(Modifier.height(12.dp))
        Row {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(50),
            )
            Spacer(Modifier.width(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(50),
            )
        }
    }
}
