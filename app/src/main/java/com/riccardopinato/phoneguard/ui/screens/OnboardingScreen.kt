package com.riccardopinato.phoneguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.ui.components.UtilityPrimaryAction
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniHero
import com.riccardopinato.phoneguard.ui.theme.OmniHeroMuted
import com.riccardopinato.phoneguard.ui.theme.OmniHeroSoft
import com.riccardopinato.phoneguard.ui.theme.OmniHeroText

private data class OnboardingPage(
    val title: String,
    val text: String
)

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit
) {
    val pages =
        listOf(
            OnboardingPage(
                title =
                    stringResource(
                        R.string.onboarding_1_title
                    ),
                text =
                    stringResource(
                        R.string.onboarding_1_text
                    )
            ),
            OnboardingPage(
                title =
                    stringResource(
                        R.string.onboarding_2_title
                    ),
                text =
                    stringResource(
                        R.string.onboarding_2_text
                    )
            ),
            OnboardingPage(
                title =
                    stringResource(
                        R.string.onboarding_3_title
                    ),
                text =
                    stringResource(
                        R.string.onboarding_3_text
                    )
            ),
            OnboardingPage(
                title =
                    stringResource(
                        R.string.onboarding_4_title
                    ),
                text =
                    stringResource(
                        R.string.onboarding_4_text
                    )
            )
        )

    var page by
        remember {
            mutableIntStateOf(0)
        }

    val current =
        pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme
                    .colorScheme
                    .background
            )
            .padding(
                horizontal = 22.dp,
                vertical = 24.dp
            ),
        verticalArrangement =
            Arrangement.SpaceBetween
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {
            pages.indices
                .forEach { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .background(
                                if (
                                    index <= page
                                ) {
                                    OmniAccent
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                }
                            )
                    )
                }
        }

        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    30.dp
                ),
            color = OmniHero,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 32.dp
                    ),
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        18.dp
                    )
            ) {
                OnboardingIllustration(page)

                Text(
                    text = current.title,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center,
                    color = OmniHeroText
                )

                Text(
                    text = current.text,
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    textAlign =
                        TextAlign.Center,
                    color = OmniHeroMuted
                )
            }
        }

        Column(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            UtilityPrimaryAction(
                label =
                    stringResource(
                        if (
                            page <
                            pages.lastIndex
                        ) {
                            R.string
                                .common_continue
                        } else {
                            R.string
                                .common_start
                        }
                    ),
                enabled = true,
                onClick = {
                    if (
                        page <
                        pages.lastIndex
                    ) {
                        page++
                    } else {
                        onCompleted()
                    }
                }
            )

            if (
                page <
                pages.lastIndex
            ) {
                TextButton(
                    onClick = onCompleted
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.common_skip
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Lightweight Compose illustration: no bitmap assets, network data or runtime
 * image dependency. Four distinct contexts help users understand activation.
 */
@Composable
private fun OnboardingIllustration(page: Int) {
    val tokens = when (page) {
        0 -> listOf(
            R.string.onboarding_visual_table,
            R.string.onboarding_visual_pocket,
            R.string.onboarding_visual_charging
        )
        1 -> listOf(
            R.string.onboarding_visual_motion,
            R.string.onboarding_visual_light,
            R.string.onboarding_visual_near
        )
        2 -> listOf(
            R.string.onboarding_visual_pin,
            R.string.onboarding_visual_biometric,
            R.string.onboarding_visual_owner
        )
        else -> listOf(
            R.string.onboarding_visual_siren,
            R.string.onboarding_visual_vibrate,
            R.string.onboarding_visual_flash
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(172.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(170.dp).clip(CircleShape)
                    .background(OmniAccent.copy(alpha = 0.09f))
            )
            Box(
                modifier = Modifier.size(135.dp).clip(CircleShape)
                    .background(OmniAccent.copy(alpha = 0.13f))
            )
            Box(
                modifier = Modifier.width(98.dp).height(157.dp)
                    .border(2.dp, OmniAccent, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(OmniHeroSoft),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PHONE GUARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmniHeroText
                    )
                    Text(
                        text = when (page) {
                            0 -> "◉"
                            1 -> "◎"
                            2 -> "✓"
                            else -> "!"
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = OmniAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tokens.forEach { token ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OmniHeroSoft
                ) {
                    Text(
                        text = stringResource(token),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OmniHeroText
                    )
                }
            }
        }
    }
}
