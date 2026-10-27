package com.riccardopinato.phoneguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionInfoDialog(
    needsCamera: Boolean,
    needsNotifications: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permessi necessari")
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Phone Guard richiede solo i permessi indispensabili alle funzioni che hai attivato."
                )

                if (needsCamera) {
                    PermissionExplanation(
                        title = "FLASH LED",
                        text = "Il permesso Fotocamera serve esclusivamente per controllare la torcia. L'app non scatta foto e non registra video."
                    )
                }

                if (needsNotifications) {
                    PermissionExplanation(
                        title = "NOTIFICHE",
                        text = "Servono per mostrare quando la protezione è attiva e quando viene rilevato movimento."
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue
            ) {
                Text("Continua")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun PermissionExplanation(
    title: String,
    text: String
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = text,
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}
