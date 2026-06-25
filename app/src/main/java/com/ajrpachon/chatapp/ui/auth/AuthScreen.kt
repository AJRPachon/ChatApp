package com.ajrpachon.chatapp.ui.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.ui.components.ChatAppOutlinedButton
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.AuthRoute
import com.ajrpachon.chatapp.ConversationListRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ConversationListRoute::class, label = "Sign In")
@NavDestination(route = AuthRoute::class)
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val vm: AuthViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> onAuthenticated()
                is AuthEffect.OpenAddGoogleAccount -> {
                    val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    }
                    context.startActivity(intent)
                }
                is AuthEffect.IntegrityFailed -> {
                    snackbar.showSnackbar(
                        message = "Este dispositivo o instalación no es de confianza. Algunas funciones pueden estar restringidas.",
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.onIntent(AuthIntent.DismissError)
        }
    }

    LaunchedEffect(state.showEmailVerification) {
        if (state.showEmailVerification) {
            snackbar.showSnackbar("Revisa tu correo para verificar tu cuenta")
            vm.onIntent(AuthIntent.DismissEmailVerification)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.needsUsername -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                UsernameSetupContent(
                    username = state.usernameInput,
                    error = state.usernameError,
                    onUsernameChange = { vm.onIntent(AuthIntent.UsernameChanged(it)) },
                    onConfirm = { vm.onIntent(AuthIntent.ConfirmUsername) },
                )
            }

            else -> LoginContent(
                state = state,
                onIntent = { vm.onIntent(it) },
                onGoogleSignIn = { vm.onIntent(AuthIntent.SignInWithGoogle(context)) },
            )
        }
    }
}

// ── Login content ──────────────────────────────────────────────────────────────

@Composable
private fun LoginContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Gradient hero header ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "ChatApp",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Conecta con tus amigos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Form card ─────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Social buttons ─────────────────────────────────────────────
                ChatAppOutlinedButton(
                    text = "Continuar con Google",
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

                // ── Divider ───────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "o continúa con email",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // ── Email/password tabs ───────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        val isSignIn = state.authMode == AuthMode.SIGN_IN
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            color = if (isSignIn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_IN)) },
                        ) {
                            Text(
                                "Iniciar sesión",
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSignIn) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSignIn) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            color = if (!isSignIn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_UP)) },
                        ) {
                            Text(
                                "Registrarse",
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!isSignIn) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isSignIn) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                EmailPasswordForm(state = state, onIntent = onIntent)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Email/password form ────────────────────────────────────────────────────────

@Composable
private fun EmailPasswordForm(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val passwordFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val isSignUp = state.authMode == AuthMode.SIGN_UP

    ChatAppTextField(
        value = state.emailInput,
        onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
        label = "Correo electrónico",
        leadingIcon = Icons.Default.Email,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
    )

    Spacer(Modifier.height(8.dp))

    ChatAppTextField(
        value = state.passwordInput,
        onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
        label = "Contraseña",
        leadingIcon = Icons.Default.Lock,
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onNext = { if (isSignUp) confirmFocus.requestFocus() },
            onDone = {
                keyboard?.hide()
                onIntent(AuthIntent.SignInWithEmail)
            },
        ),
        modifier = Modifier.focusRequester(passwordFocus),
    )

    AnimatedVisibility(visible = isSignUp) {
        Column {
            Spacer(Modifier.height(8.dp))
            ChatAppTextField(
                value = state.confirmPasswordInput,
                onValueChange = { onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                label = "Confirmar contraseña",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirm) "Ocultar" else "Mostrar",
                        )
                    }
                },
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onIntent(AuthIntent.SignUpWithEmail)
                }),
                modifier = Modifier.focusRequester(confirmFocus),
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    ChatAppPrimaryButton(
        text = if (isSignUp) "Crear cuenta" else "Iniciar sesión",
        onClick = {
            keyboard?.hide()
            if (isSignUp) onIntent(AuthIntent.SignUpWithEmail) else onIntent(AuthIntent.SignInWithEmail)
        },
        enabled = state.emailInput.isNotBlank() && state.passwordInput.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    )

    if (!isSignUp) {
        if (state.showRegisterSuggestion) {
            TextButton(
                onClick = { onIntent(AuthIntent.SwitchToRegister) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("¿No tienes cuenta? Regístrate aquí")
            }
        } else {
            TextButton(
                onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_UP)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    } else {
        TextButton(
            onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_IN)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

// ── Username setup ─────────────────────────────────────────────────────────────

@Composable
private fun UsernameSetupContent(
    username: String,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Text("Elige tu nombre de usuario", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "3-20 caracteres, minúsculas, dígitos o guiones bajos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        ChatAppTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = "@usuario",
            isError = error != null,
            supportingText = error,
        )
        Spacer(Modifier.height(16.dp))
        ChatAppPrimaryButton(
            text = "Confirmar",
            onClick = onConfirm,
            enabled = username.length >= 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
