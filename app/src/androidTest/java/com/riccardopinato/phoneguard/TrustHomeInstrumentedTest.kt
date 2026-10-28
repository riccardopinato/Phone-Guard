package com.riccardopinato.phoneguard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.service.AlarmStateStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-visible UX truth, independent of manual sensor certification. */
@RunWith(AndroidJUnit4::class)
class TrustHomeInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun disarmedHomeMakesSmartArmAndAutoGuardDiscoverable() {
        val app = ApplicationProvider.getApplicationContext<PhoneGuardApp>()
        runBlocking { app.container.settingsRepository.setOnboardingCompleted(true) }
        rule.runOnIdle { AlarmStateStore.reset() }
        val unprotected = app.getString(R.string.home_status_unprotected)
        rule.waitUntil(15_000L) {
            rule.onAllNodesWithText(unprotected).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(unprotected).assertIsDisplayed()
        rule.onNodeWithText(app.getString(R.string.smart_arm_action)).assertExists()
        val manage = app.getString(R.string.home_open_auto_guard)
        rule.onNodeWithText(manage).performClick()
        rule.waitUntil(15_000L) {
            rule.onAllNodesWithText(app.getString(R.string.auto_guard_title))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(app.getString(R.string.auto_guard_standby_disclaimer))
            .assertExists()
    }
}
