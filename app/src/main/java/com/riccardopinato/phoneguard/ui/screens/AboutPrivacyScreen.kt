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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.BuildConfig
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.theme.OmniAccent

@Composable
fun AboutPrivacyScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "‹ Indietro",
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                style = MaterialTheme.typography.labelLarge,
                color = OmniAccent
            )

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "TABLETOP",
                style = MaterialTheme.typography.labelMedium,
                color = OmniAccent,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Info e privacy",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        InfoPanel(
            title = "Come funziona",
            text =
                "Quando attivi la protezione, l'app usa i sensori di movimento del dispositivo per rilevare spostamenti o sollevamenti. Se viene rilevato un movimento significativo, può attivare sirena, vibrazione e flash LED in base alle tue impostazioni."
        )

        InfoPanel(
            title = "Dati e privacy",
            text =
                "Il rilevamento del movimento viene elaborato sul dispositivo. L'app non registra audio, non usa la posizione e non scatta fotografie. Lo storico degli eventi resta nella memoria locale dell'app."
        )

        InfoPanel(
            title = "Fotocamera",
            text =
                "Il permesso Fotocamera viene utilizzato esclusivamente per consentire il controllo della torcia/flash LED tramite le API Android. Phone Guard non usa la fotocamera per acquisire immagini o video."
        )

        InfoPanel(
            title = "Nessun account",
            text =
                "Phone Guard non richiede registrazione o login. Le funzioni principali operano localmente sul dispositivo e non dipendono da un account proprietario."
        )

        InfoPanel(
            title = "Backup Android",
            text =
                "Le impostazioni non sensibili possono essere incluse nei meccanismi Android di backup o trasferimento dispositivo. PIN, hash del PIN e cronologia degli eventi sono esclusi dal backup."
        )

        InfoPanel(
            title = "Servizio in primo piano",
            text =
                "Quando armi l'allarme, viene avviato un servizio in primo piano con una notifica permanente. È necessario per continuare a monitorare i sensori mentre lo schermo è spento o l'app non è visibile. Il servizio termina quando disattivi la protezione."
        )

        InfoPanel(
            title = "Limiti",
            text =
                "L'app è uno strumento di deterrenza e segnalazione locale. Non può impedire fisicamente un furto, lo spegnimento del dispositivo, il force-stop dell'app o altre azioni effettuate tramite il sistema operativo."
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
