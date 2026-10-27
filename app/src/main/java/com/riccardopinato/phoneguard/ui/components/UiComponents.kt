package com.riccardopinato.phoneguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.ui.theme.OmniAccent

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = OmniAccent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val valid =
        pin.matches(Regex("\\d{4,6}")) &&
            pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Imposta PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Scegli un PIN da 4 a 6 cifre. " +
                        "Non viene salvato in chiaro."
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    )
                )

                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            confirm = it
                        }
                    },
                    label = { Text("Conferma PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    )
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(pin) }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun UnlockDialog(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onBiometric: () -> Unit,
    title: String = "Disattiva allarme",
    message: String = "Inserisci il PIN oppure usa la biometria.",
    confirmLabel: String = "Sblocca"
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(message)

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    )
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (biometricEnabled && biometricAvailable) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBiometric
                    ) {
                        Text("Usa biometria")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length in 4..6,
                onClick = { onPinSubmit(pin) }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}
