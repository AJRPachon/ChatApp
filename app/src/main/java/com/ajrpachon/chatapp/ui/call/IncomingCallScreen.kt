package com.ajrpachon.chatapp.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.CallType
import com.ajrpachon.chatapp.ui.theme.CallAcceptedGreen
import com.ajrpachon.chatapp.ui.theme.CallBackground

@Composable
fun IncomingCallScreen(
    call: CallBO,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBackground.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (call.type == CallType.VIDEO) {
                    stringResource(R.string.incoming_call_video_incoming)
                } else {
                    stringResource(R.string.incoming_call_voice_incoming)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )

            CallPartyHeader(call.callerName)

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallControlButton(
                        onClick = onReject,
                        containerColor = MaterialTheme.colorScheme.error,
                        iconTint = Color.White,
                        size = 64.dp,
                    ) {
                        Icon(
                            Icons.Default.PhoneDisabled,
                            contentDescription = stringResource(R.string.incoming_call_reject_content_description),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.incoming_call_reject_label),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallControlButton(
                        onClick = onAccept,
                        containerColor = CallAcceptedGreen,
                        iconTint = Color.White,
                        size = 64.dp,
                    ) {
                        Icon(
                            if (call.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Phone,
                            contentDescription = stringResource(R.string.incoming_call_accept_content_description),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.incoming_call_accept_label),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
