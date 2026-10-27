package com.riccardopinato.phoneguard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.BuildConfig
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.theme.OmniAccent

@Composable
fun AboutPrivacyScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 18.dp,
                vertical = 16.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement
                    .SpaceBetween
        ) {
            Text(
                text =
                    stringResource(
                        R.string.common_back
                    ),
                modifier = Modifier
                    .clickable(
                        onClick = onBack
                    )
                    .padding(
                        vertical = 8.dp,
                        horizontal = 2.dp
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelLarge,
                color = OmniAccent
            )

            Text(
                text =
                    stringResource(
                        R.string.version_format,
                        BuildConfig
                            .VERSION_NAME
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    1.dp
                )
        ) {
            Text(
                text =
                    stringResource(
                        R.string.about_brand
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color = OmniAccent,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    stringResource(
                        R.string.about_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge
            )
        }

        InfoPanel(
            title =
                stringResource(
                    R.string.about_how_title
                ),
            text =
                stringResource(
                    R.string.about_how_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_privacy_title
                ),
            text =
                stringResource(
                    R.string.about_privacy_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_camera_title
                ),
            text =
                stringResource(
                    R.string.about_camera_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_account_title
                ),
            text =
                stringResource(
                    R.string.about_account_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_backup_title
                ),
            text =
                stringResource(
                    R.string.about_backup_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_fgs_title
                ),
            text =
                stringResource(
                    R.string.about_fgs_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_outputs_title
                ),
            text =
                stringResource(
                    R.string.about_outputs_text
                )
        )

        InfoPanel(
            title =
                stringResource(
                    R.string.about_limits_title
                ),
            text =
                stringResource(
                    R.string.about_limits_text
                )
        )
    }
}

@Composable
private fun InfoPanel(
    title: String,
    text: String
) {
    UtilityPanel {
        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = text,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}
