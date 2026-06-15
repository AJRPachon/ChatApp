package com.ajrpachon.chatapp.ui.navgraph

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.AuthRoute
import com.ajrpachon.chatapp.CallRoute
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.ConversationListRoute
import com.ajrpachon.chatapp.CreateGroupRoute
import com.ajrpachon.chatapp.GroupInfoRoute
import com.ajrpachon.chatapp.InvitationsRoute
import com.ajrpachon.chatapp.NewChatRoute
import com.ajrpachon.chatapp.ProfileRoute
import com.ajrpachon.chatapp.UserInfoRoute
import com.github.skydoves.navgraph.annotations.NavPreview

// ── Shared preview theme ──────────────────────────────────────────────────────

private val PreviewPrimary = Color(0xFF6B71B8)
private val PreviewSurface = Color(0xFFFBF8FF)
private val PreviewOnSurface = Color(0xFF1B1B1F)
private val PreviewSurfaceVariant = Color(0xFFE4E1EC)
private val PreviewContainer = Color(0xFFEFECF5)

private val NavPreviewColors = lightColorScheme(
    primary = PreviewPrimary,
    surface = PreviewSurface,
    onSurface = PreviewOnSurface,
    surfaceVariant = PreviewSurfaceVariant,
    surfaceContainer = PreviewContainer,
)

@Composable
private fun NavPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NavPreviewColors, content = content)
}

// ── Shared stub composables ───────────────────────────────────────────────────

@Composable
private fun StubAvatar(initials: String, color: Color = PreviewPrimary, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StubConversationRow(name: String, preview: String, time: String, unread: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StubAvatar(name.take(1))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(preview, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF787680), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = Color(0xFF787680))
            if (unread > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(PreviewPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$unread", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StubMessageBubble(text: String, isOwn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isOwn) PreviewPrimary else PreviewSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = text,
                color = if (isOwn) Color.White else PreviewOnSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Auth preview ──────────────────────────────────────────────────────────────

@NavPreview(route = AuthRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun AuthScreenPreview() {
    NavPreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFDFE0FF), PreviewSurface)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(PreviewPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("ChatApp", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = PreviewPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Sign in to continue", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF787680))
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = "user@email.com", onValueChange = {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = "••••••••", onValueChange = {}, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Sign In")
            }
        }
    }
}

// ── ConversationList preview ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = ConversationListRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun ConversationListScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chats", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                item { StubConversationRow("Alice Johnson", "See you tomorrow!", "10:30", unread = 2) }
                item { StubConversationRow("Team Dev", "PR merged ✅", "09:15", unread = 5) }
                item { StubConversationRow("Bob Martinez", "Thanks!", "Yesterday") }
                item { StubConversationRow("Design Squad", "New mockups shared", "Mon") }
                item { StubConversationRow("Carol White", "Are you free?", "Sun") }
                item { StubConversationRow("Support", "Your ticket is resolved", "Sat") }
            }
        }
    }
}

// ── Chat preview ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = ChatRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun ChatScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StubAvatar("A")
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Alice Johnson", fontWeight = FontWeight.SemiBold)
                                Text("Online", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5E8B6D))
                            }
                        }
                    },
                    actions = {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.padding(8.dp))
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.padding(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(PreviewSurface).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = "Type a message…",
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(PreviewPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    }
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                Spacer(Modifier.height(8.dp))
                StubMessageBubble("Hey! How's it going?", isOwn = false)
                StubMessageBubble("Great, working on the new feature 🚀", isOwn = true)
                StubMessageBubble("Sounds exciting! Can't wait to see it", isOwn = false)
                StubMessageBubble("Almost done, will share a demo soon", isOwn = true)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Invitations preview ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = InvitationsRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun InvitationsScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Invitations", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                repeat(3) { i ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StubAvatar(('A' + i).toString(), color = Color(0xFF7986CB))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("User ${i + 1}", fontWeight = FontWeight.SemiBold)
                            Text("Sent you an invitation", style = MaterialTheme.typography.bodySmall, color = Color(0xFF787680))
                        }
                        FilledTonalButton(onClick = {}) { Text("Accept") }
                    }
                }
            }
        }
    }
}

// ── NewChat preview ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = NewChatRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun NewChatScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("New Chat", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Search or enter email…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                )
                Spacer(Modifier.height(16.dp))
                repeat(4) { i ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StubAvatar(('B' + i).toString(), color = Color(0xFF66BB6A))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Contact ${i + 1}", fontWeight = FontWeight.SemiBold)
                            Text("contact${i + 1}@email.com", style = MaterialTheme.typography.bodySmall, color = Color(0xFF787680))
                        }
                    }
                }
            }
        }
    }
}

// ── Profile preview ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = ProfileRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun ProfileScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                StubAvatar("AJ", size = 80)
                Spacer(Modifier.height(16.dp))
                Text("AJ Pachon", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("ajrpachon@email.com", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF787680))
                Spacer(Modifier.height(32.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PreviewContainer,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Display Name", style = MaterialTheme.typography.labelMedium, color = Color(0xFF787680))
                        Text("AJ Pachon", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(32.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Changes")
                }
            }
        }
    }
}

// ── Call preview ──────────────────────────────────────────────────────────────

@NavPreview(route = CallRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun CallScreenPreview() {
    NavPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1a1a2e), Color(0xFF16213e)))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StubAvatar("AJ", color = Color(0xFF6B71B8), size = 100)
                Spacer(Modifier.height(24.dp))
                Text("Alice Johnson", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("00:42", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(64.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFBA1A1A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

// ── CreateGroup preview ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = CreateGroupRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun CreateGroupScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("New Group", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(PreviewContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = PreviewPrimary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = "Team Alpha", onValueChange = {}, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = "Project coordination", onValueChange = {}, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                Text("Add Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(12.dp))
                repeat(3) { i ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StubAvatar(('C' + i).toString(), color = Color(0xFFAB47BC))
                        Spacer(Modifier.width(12.dp))
                        Text("Member ${i + 1}", fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Create Group")
                }
            }
        }
    }
}

// ── GroupInfo preview ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = GroupInfoRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun GroupInfoScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Group Info", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF7986CB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Team Alpha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Project coordination", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF787680))
                Spacer(Modifier.height(24.dp))
                Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                repeat(4) { i ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StubAvatar(('D' + i).toString(), color = Color(0xFFEF5350))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Member ${i + 1}", fontWeight = FontWeight.SemiBold)
                            if (i == 0) Text("Admin", style = MaterialTheme.typography.labelSmall, color = PreviewPrimary)
                        }
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF787680))
                    }
                }
            }
        }
    }
}

// ── UserInfo preview ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@NavPreview(route = UserInfoRoute::class, primary = true)
@Preview(showBackground = true)
@Composable
internal fun UserInfoScreenPreview() {
    NavPreviewTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("User Info", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PreviewSurface),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                StubAvatar("BM", color = Color(0xFF26C6DA), size = 96)
                Spacer(Modifier.height(16.dp))
                Text("Bob Martinez", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("bob.martinez@email.com", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF787680))
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFBCEDCE),
                ) {
                    Text("Online", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color(0xFF0A2118), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalButton(onClick = {}) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call")
                    }
                    Button(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Message")
                    }
                }
            }
        }
    }
}
