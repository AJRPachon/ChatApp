package com.ajrpachon.chatapp.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.common.AppSplashScreen
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.ajrpachon.chatapp.ui.theme.Signal_SurfaceDark
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.AuthRoute
import com.ajrpachon.chatapp.ConversationListRoute
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.ui.components.ChatAppOutlinedButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextButton
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@NavEdge(to = ConversationListRoute::class, label = "Sign In")
@NavDestination(route = AuthRoute::class)
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val vm: AuthViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val googleCredentialFetcher: GoogleCredentialFetcher = koinInject()
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
                is AuthEffect.RequestGoogleCredential -> launch {
                    try {
                        when (val result = googleCredentialFetcher.fetch(context, effect.hashedNonce)) {
                            is GoogleCredentialResult.Token -> vm.onIntent(AuthIntent.GoogleTokenReceived(result.idToken))
                            is GoogleCredentialResult.NoCredential -> vm.onIntent(AuthIntent.GoogleSignInFailed(message = null, noCredential = true))
                            is GoogleCredentialResult.Failed -> vm.onIntent(AuthIntent.GoogleSignInFailed(message = result.message, noCredential = false))
                        }
                    } catch (e: CancellationException) {
                        // The screen left while the credential sheet was up: let the ViewModel stop its spinner.
                        vm.onIntent(AuthIntent.GoogleSignInCancelled)
                        throw e
                    }
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
            snackbar.showSnackbar(it.asString(context))
            vm.onIntent(AuthIntent.DismissError)
        }
    }

    LaunchedEffect(state.showEmailVerification) {
        if (state.showEmailVerification) {
            snackbar.showSnackbar(checkEmailVerificationMessage)
            vm.onIntent(AuthIntent.DismissEmailVerification)
        }
    }

    AuthContent(state = state, snackbarHostState = snackbar, onIntent = vm::onIntent)
}

@Composable
fun AuthContent(
    state: AuthState,
    snackbarHostState: SnackbarHostState,
    onIntent: (AuthIntent) -> Unit,
) {
    // AuthViewModel's own init block (a second integrity check + session restore) keeps this
    // true for a beat after MainActivity's splash has already come down. Render the same
    // AppSplashScreen rather than a bare spinner: MainActivity's splash gate only waits on ITS
    // OWN integrity check + initial route, not on this screen's, so without this the branded
    // splash would hand off to an unbranded spinner on a blank screen for however long this
    // second check takes — exactly the kind of jump the design is meant to hide. AppSplashScreen
    // has no reveal animation to replay, so mounting it again here is a silent continuation, not
    // a restart.
    if (state.isLoading) {
        AppSplashScreen(darkTheme = MaterialTheme.colorScheme.background == Signal_SurfaceDark)
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        when {
            state.needsMfaChallenge -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                MfaChallengeContent(
                    code = state.mfaCodeInput,
                    error = state.mfaError,
                    isLoading = state.mfaIsLoading,
                    onCodeChange = { onIntent(AuthIntent.MfaCodeChanged(it)) },
                    onVerify = { onIntent(AuthIntent.VerifyMfaCode) },
                )
            }

            state.needsUsername -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                UsernameSetupContent(
                    username = state.usernameInput,
                    error = state.usernameError,
                    onUsernameChange = { onIntent(AuthIntent.UsernameChanged(it)) },
                    onConfirm = { onIntent(AuthIntent.ConfirmUsername) },
                )
            }

            else -> LoginContent(
                state = state,
                onIntent = onIntent,
                onGoogleSignIn = { onIntent(AuthIntent.SignInWithGoogle) },
                contentPadding = innerPadding,
            )
        }
    }
}

@Preview(name = "Sign in", showBackground = true)
@Composable
internal fun AuthSignInPreview() {
    ChatAppTheme {
        AuthContent(
            state = AuthState(isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}

@Preview(name = "Sign up", showBackground = true)
@Composable
internal fun AuthSignUpPreview() {
    ChatAppTheme {
        AuthContent(
            state = AuthState(isLoading = false, authMode = AuthMode.SIGN_UP),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}

@Preview(name = "Username setup", showBackground = true)
@Composable
internal fun AuthUsernameSetupPreview() {
    ChatAppTheme {
        AuthContent(
            state = AuthState(isLoading = false, needsUsername = true, usernameInput = "ana_g"),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}

@Preview(name = "MFA challenge", showBackground = true)
@Composable
internal fun AuthMfaChallengePreview() {
    ChatAppTheme {
        AuthContent(
            state = AuthState(isLoading = false, needsMfaChallenge = true, mfaCodeInput = "123"),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            // Only the BOTTOM inset (nav bar) is consumed here — the top inset lives on the
            // hero's own statusBarsPadding below, applied to its content instead of the whole
            // column, so the gradient background isn't pushed down before it paints.
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        // ── Bottom-pinned switch-mode link, keyboard-safe ───────────────────────
        // A weight(1f) spacer can't live inside a scrollable Column — verticalScroll gives its
        // content infinite height, which crashes weight outright. Instead, measure the hero+card
        // block and the link block, and fill the *actual* leftover space by hand: the link sits
        // pinned to the true bottom only while there's room to spare. The moment `maxHeight`
        // shrinks below what the content needs — e.g. the keyboard opening, since it's inside
        // imePadding() above — the filler collapses to 0dp and the link falls back into the
        // normal scroll flow after the card, instead of staying glued right above the keyboard.
        val density = LocalDensity.current
        val availableHeightPx = with(density) { maxHeight.roundToPx() }
        var heroCardHeightPx by remember { mutableIntStateOf(0) }
        var linkBlockHeightPx by remember { mutableIntStateOf(0) }
        val targetFillerHeight = with(density) {
            (availableHeightPx - heroCardHeightPx - linkBlockHeightPx).coerceAtLeast(0).toDp()
        }
        // heroCardHeightPx always lags one frame behind the real layout (onSizeChanged only
        // reports it after the fact), so while the card's height is itself animating — e.g. the
        // confirm-password field expanding in on sign-up — targetFillerHeight corrects itself
        // discretely every frame instead of tracking smoothly, which reads as the link block
        // jumping. Animating the filler itself smooths that catch-up into one continuous move.
        val fillerHeight by animateDpAsState(targetValue = targetFillerHeight, label = "auth_filler_height")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.onSizeChanged { heroCardHeightPx = it.height }) {
                // ── Gradient hero ───────────────────────────────────────────────
                // No fixed height: this Box wraps the hero Column's own (now taller) height
                // instead of a hardcoded dp value, so it always extends exactly as far as the
                // content needs — including the status bar's real height on this device,
                // whatever that is. The old version fixed this Box at 260dp AND padded the
                // *outer* column by the Scaffold's status-bar inset AND padded this Box's own
                // content by statusBarsPadding again — two top insets stacked, leaving a plain,
                // un-gradiented strip above a hard seam where the color cut in abruptly. Now
                // there's exactly one statusBarsPadding, on the content, and the gradient
                // behind it simply follows.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 20.dp, bottom = 36.dp),
                    ) {
                        // Fondo siempre oscuro (#0E1516), como en el resto de apariciones del
                        // glifo de marca (icono de lanzador, splash) — el contenedor del icono no
                        // sigue el tema claro/oscuro de la app, es parte fija de la identidad visual.
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 26.dp, bottom = 6.dp)
                                .size(90.dp)
                        )
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

                // ── Form card ───────────────────────────────────────────────────
                // The form lives in its own floating card instead of flush against the hero
                // above — separates the brand moment (gradient, logo, title) from the task (the
                // fields), and gives the screen a resting composition instead of one long
                // stacked column that ends in a lot of empty space below.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // ── Social buttons ─────────────────────────────────────
                        // Not ChatAppOutlinedButton: its leadingIcon always renders through
                        // Icon(), which force-tints the whole vector with LocalContentColor —
                        // fine for the app's own monochrome icons, but it would flatten
                        // Google's real multi-color "G" mark into a single-color blob. Image()
                        // doesn't tint, so the vector's own per-path fillColors (ic_google.xml)
                        // survive — same shape/padding as ChatAppOutlinedButton otherwise, for
                        // visual consistency with the rest of the screen.
                        ChatAppOutlinedButton(
                            text = stringResource(R.string.auth_continue_with_google),
                            onClick = onGoogleSignIn,
                            modifier = Modifier.fillMaxWidth(),
                            leadingContent = {
                                Image(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_google),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── Divider ───────────────────────────────────────────
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

                        // ── Email/password tabs ─────────────────────────────────
                        // The selected tab must match the track's own height exactly — both use
                        // CircleShape, which is a percent radius computed from each
                        // element's OWN size. Insetting the Row vertically (as well as
                        // horizontally) used to give the inner tab a shorter height than the
                        // track, so its "fully rounded" radius came out a few dp smaller than the
                        // track's — a correct capsule on its own, but visibly less round than its
                        // parent right next to it. Horizontal-only padding keeps the tab's height
                        // identical to the track's, so both resolve to the same absolute radius.
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                val isSignIn = state.authMode == AuthMode.SIGN_IN
                                AuthModeTab(
                                    text = stringResource(R.string.auth_sign_in_tab),
                                    selected = isSignIn,
                                    onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_IN)) },
                                )
                                AuthModeTab(
                                    text = stringResource(R.string.auth_sign_up_tab),
                                    selected = !isSignIn,
                                    onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_UP)) },
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        EmailPasswordForm(state = state, onIntent = onIntent)
                    }
                }
            }

            // Fills the leftover space (0dp once the keyboard eats into it) so the link below
            // sits at the true bottom when there's room, and falls back to trailing the card
            // with normal scroll otherwise — see the comment above this composable's root.
            Spacer(Modifier.height(fillerHeight))

            Column(modifier = Modifier.onSizeChanged { linkBlockHeightPx = it.height }) {
                AuthSwitchModeLink(state = state, onIntent = onIntent)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AuthSwitchModeLink(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    val isSignUp = state.authMode == AuthMode.SIGN_UP
    if (!isSignUp) {
        if (state.showRegisterSuggestion) {
            ChatAppTextButton(
                text = stringResource(R.string.auth_no_account_register_here),
                onClick = { onIntent(AuthIntent.SwitchToRegister) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ChatAppTextButton(
                text = stringResource(R.string.auth_no_account_register),
                onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_UP)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        ChatAppTextButton(
            text = stringResource(R.string.auth_have_account_sign_in),
            onClick = { onIntent(AuthIntent.ToggleMode(AuthMode.SIGN_IN)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Sign in / Sign up track segment — see the shape comment above its call site for why the
// selected fill must share the track's own height instead of insetting on all sides.
@Composable
private fun RowScope.AuthModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick,
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
    // Switch-mode link now lives outside the card — see AuthSwitchModeLink, called from
    // LoginContent after this form's Surface.
}

// ── MFA Challenge ──────────────────────────────────────────────────────────────

@Composable
private fun MfaChallengeContent(
    code: String,
    error: UiText?,
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
            supportingText = error?.asString(),
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
    error: UiText?,
    onUsernameChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .safeDrawingPadding(),
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
            supportingText = error?.asString(),
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
