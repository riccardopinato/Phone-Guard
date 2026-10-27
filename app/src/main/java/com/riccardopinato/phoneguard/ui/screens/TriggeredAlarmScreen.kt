package com.riccardopinato.phoneguard.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.ui.components.UtilityPrimaryAction

private val WarningDark = Color(0xFF250000)
private val WarningBright = Color(0xFFD50000)

@Composable
fun TriggeredAlarmScreen(
    pulseEnabled: Boolean = true,
    onUnlock: () -> Unit
) {
    val transition =
        rememberInfiniteTransition(
            label = "phone-guard-warning"
        )

    val background by
        transition.animateColor(
            initialValue = WarningDark,
            targetValue =
                if (pulseEnabled) {
                    WarningBright
                } else {
                    WarningDark
                },
            animationSpec =
                infiniteRepeatable(
                    animation = tween(320),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "warning-background"
        )

    val warningAlpha by
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue =
                if (pulseEnabled) 1f else 0.85f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(320),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "warning-alpha"
        )

    ForceWarningBrightness()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Text(
            text = "WARNING!",
            modifier = Modifier.alpha(warningAlpha),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PROTECTED DEVICE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text =
                    "Movimento non autorizzato rilevato. " +
                        "Sirena, vibrazione e segnalazioni visive sono attive.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )
        }

        UtilityPrimaryAction(
            label = "SBLOCCA E DISATTIVA",
            enabled = true,
            destructive = true,
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth(0.86f)
        )
    }
}

@Composable
private fun ForceWarningBrightness() {
    val view = LocalView.current
    val activity = view.context.findActivity()

    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val window = activity.window
            val oldBrightness =
                window.attributes.screenBrightness
            val hadKeepScreenOn =
                window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

            val attrs = window.attributes
            attrs.screenBrightness = 1f
            window.attributes = attrs
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            onDispose {
                val restore = window.attributes
                restore.screenBrightness = oldBrightness
                window.attributes = restore

                if (!hadKeepScreenOn) {
                    window.clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper ->
            baseContext.findActivity()
        else -> null
    }
