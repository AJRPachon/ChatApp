package com.ajrpachon.chatapp.ui.applock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val vm: AppLockViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricTitle = stringResource(R.string.applock_biometric_title)
    val biometricSubtitle = stringResource(R.string.applock_biometric_subtitle)

    // The actual BiometricPrompt must run in the screen (needs FragmentActivity + executor).
    // The ViewModel emits Effect.LaunchBiometric; callbacks dispatch intents back to the VM.
    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                vm.onIntent(AppLockIntent.AuthSucceeded)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    vm.onIntent(AppLockIntent.AuthError(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                vm.onIntent(AppLockIntent.AuthFailed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(biometricTitle)
            .setSubtitle(biometricSubtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }

    // Collect one-shot effects from the ViewModel
    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is AppLockEffect.LaunchBiometric -> launchBiometric()
                is AppLockEffect.Authenticated -> onUnlocked()
            }
        }
    }

    // Auto-trigger biometric prompt on first composition if the device supports it
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            vm.requestBiometric()
        }
    }

    AppLockContent(
        errorMessage = state.errorMessage,
        onUnlockClick = { vm.requestBiometric() },
    )
}

// Pure UI, no ViewModel/Koin — split out so it can be rendered in @Preview below without a
// Koin context, which isn't available in Android Studio's preview renderer.
@Composable
private fun AppLockContent(
    errorMessage: String?,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp)
            .testTag("applock_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.applock_locked_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.applock_locked_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        ChatAppPrimaryButton(
            text = stringResource(R.string.applock_unlock_with_fingerprint),
            onClick = onUnlockClick,
            leadingIcon = Icons.Default.Fingerprint,
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
internal fun AppLockScreenPreview() {
    ChatAppTheme(darkTheme = false) {
        AppLockContent(errorMessage = null, onUnlockClick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
internal fun AppLockScreenDarkPreview() {
    ChatAppTheme(darkTheme = true) {
        AppLockContent(errorMessage = null, onUnlockClick = {})
    }
}

@Preview(name = "With error", showBackground = true)
@Composable
internal fun AppLockScreenErrorPreview() {
    // errorMessage comes from BiometricPrompt at runtime (a system-provided CharSequence, not
    // a string resource) — hardcoded here just to preview how that state looks.
    ChatAppTheme(darkTheme = false) {
        AppLockContent(errorMessage = "No se pudo verificar la huella", onUnlockClick = {})
    }
}
