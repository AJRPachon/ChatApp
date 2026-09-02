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
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
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
    val integrityFailedMessage = stringResource(R.string.auth_integrity_failed_message)
    val checkEmailVerificationMessage = stringResource(R.string.auth_check_email_verification)

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
                        message = integrityFailedMessage,
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
            snackbar.showSnackbar(checkEmailVerificationMessage)
            vm.onIntent(AuthIntent.DismissEmailVerification)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.needsMfaChallenge -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                MfaChallengeContent(
                    code = state.mfaCodeInput,
                    error = state.mfaError,
                    isLoading = state.mfaIsLoading,
                    onCodeChange = { vm.onIntent(AuthIntent.MfaCodeChanged(it)) },
                    onVerify = { vm.onIntent(AuthIntent.VerifyMfaCode) },
                )
            }

            state.needsUsername -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                contentPadding = innerPadding,
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
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
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
                    stringResource(R.string.auth_app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.auth_tagline),
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
                    text = stringResource(R.string.auth_continue_with_google),
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
                        stringResource(R.string.auth_or_continue_with_email),
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
                                stringResource(R.string.auth_sign_in_tab),
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
                                stringResource(R.string.auth_sign_up_tab),
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
        label = stringResource(R.string.auth_email_label),
        leadingIcon = Icons.Default.Email,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
        modifier = Modifier.testTag("auth_email_field"),
    )

    Spacer(Modifier.height(8.dp))

    ChatAppTextField(
        value = state.passwordInput,
        onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
        label = stringResource(R.string.auth_password_label),
        leadingIcon = Icons.Default.Lock,
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) {
                        stringResource(R.string.auth_hide_password)
                    } else {
                        stringResource(R.string.auth_show_password)
                    },
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
        modifier = Modifier
            .focusRequester(passwordFocus)
            .testTag("auth_password_field"),
    )

    AnimatedVisibility(visible = isSignUp) {
        Column {
            Spacer(Modifier.height(8.dp))
            ChatAppTextField(
                value = state.confirmPasswordInput,
                onValueChange = { onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.auth_confirm_password_label),
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirm) {
                                stringResource(R.string.auth_hide_password)
                            } else {
                                stringResource(R.string.auth_show_password)
                            },
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
        text = if (isSignUp) {
            stringResource(R.string.auth_create_account)
        } else {
            stringResource(R.string.auth_sign_in_button)
        },
        onClick = {
            keyboard?.hide()
            if (isSignUp) onIntent(AuthIntent.SignUpWithEmail) else onIntent(AuthIntent.SignInWithEmail)
        },
        enabled = state.emailInput.isNotBlank() && state.passwordInput.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_submit_button"),
    )

    if (!isSignUp) {
        if (state.showRegisterSuggestion) {
            TextButton(
                onClick = { onIntent(AuthIntent.SwitchToRegister) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_no_account_register_here))
            }
        } else {
            TextButton(
                onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_UP)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_no_account_register))
            }
        }
    } else {
        TextButton(
            onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_IN)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.auth_have_account_sign_in))
        }
    }
}

// ── MFA Challenge ──────────────────────────────────────────────────────────────

@Composable
private fun MfaChallengeContent(
    code: String,
    error: String?,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .safeDrawingPadding(),
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.auth_mfa_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_mfa_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ChatAppTextField(
            value = code,
            onValueChange = { if (it.length <= 6) onCodeChange(it) },
            label = stringResource(R.string.auth_totp_code_label),
            leadingIcon = Icons.Default.Lock,
            isError = error != null,
            supportingText = error,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboard?.hide()
                onVerify()
            }),
        )
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            ChatAppPrimaryButton(
                text = stringResource(R.string.auth_verify_button),
                onClick = {
                    keyboard?.hide()
                    onVerify()
                },
                enabled = code.length == 6,
                modifier = Modifier.fillMaxWidth(),
            )
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
        Text(stringResource(R.string.auth_choose_username_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_username_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        ChatAppTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.auth_username_label),
            isError = error != null,
            supportingText = error,
        )
        Spacer(Modifier.height(16.dp))
        ChatAppPrimaryButton(
            text = stringResource(R.string.auth_confirm_button),
            onClick = onConfirm,
            enabled = username.length >= 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
