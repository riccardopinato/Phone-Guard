package com.riccardopinato.phoneguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.ui.components.UtilityPrimaryAction

@Composable
fun OwnerCheckScreen(
    mode: ProtectionMode,
    onVerifyOwner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF4A2F00)
            )
            .padding(
                horizontal = 24.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                20.dp,
                Alignment.CenterVertically
            )
    ) {
        Text(
            text =
                stringResource(
                    R.string
                        .owner_check_title
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineLarge,
            fontWeight =
                FontWeight.Black,
            color = Color.White,
            textAlign =
                TextAlign.Center
        )

        Text(
            text =
                stringResource(
                    R.string
                        .owner_check_body,
                    mode.label
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            color =
                Color.White.copy(
                    alpha = 0.94f
                ),
            textAlign =
                TextAlign.Center
        )

        Text(
            text =
                stringResource(
                    R.string
                        .owner_check_timeout
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                Color.White.copy(
                    alpha = 0.78f
                ),
            textAlign =
                TextAlign.Center
        )

        UtilityPrimaryAction(
            label =
                stringResource(
                    R.string
                        .owner_check_action
                ),
            enabled = true,
            onClick =
                onVerifyOwner,
            modifier =
                Modifier.fillMaxWidth(
                    0.9f
                )
        )
    }
}
