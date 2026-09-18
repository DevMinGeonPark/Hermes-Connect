package com.hermesandroid.relay.screenshots

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermesandroid.relay.R

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hermesandroid.relay.ui.components.ConnectDestination
import com.hermesandroid.relay.ui.components.ConnectNavigationBar
import com.hermesandroid.relay.ui.components.VoiceModeOverlay
import com.hermesandroid.relay.ui.components.VOICE_FOCUS_SPLIT_LAYOUT_TEST_TAG
import com.hermesandroid.relay.ui.components.ProfileSwitcherSheet
import com.hermesandroid.relay.data.Profile
import com.hermesandroid.relay.data.ProfilePresentation
import com.hermesandroid.relay.viewmodel.ConnectionViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import com.hermesandroid.relay.ui.theme.HermesRelayTheme
import com.hermesandroid.relay.viewmodel.VoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Production Compose components with no transport, recording, or audio-provider calls. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-rKR-w390dp-h840dp-xhdpi")
class ConnectUiScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun voiceDark() = voice("dark", "voice-dark")
    @Test fun voiceLight() = voice("light", "voice-light")

    @Test fun profilesKeepTheirServerIdentity() {
        val profiles = listOf(Profile("work", "", displayName = "업무"),
            Profile("personal", "", displayName = "개인"), Profile("research", "", displayName = "리서치"))
        val vm = mockk<ConnectionViewModel>(relaxed = true)
        every { vm.profileIconFlow(any()) } returns MutableStateFlow(null)
        every { vm.serverDefaultDisplayProfile } returns MutableStateFlow(null)
        val selected = mutableStateOf<Profile?>(profiles.first())
        compose.setContent {
            HermesRelayTheme(themePreference = "dark") {
                Surface(Modifier.fillMaxSize()) {
                    ProfileSwitcherSheet(vm, profiles, selected.value, selected.value, ProfilePresentation(),
                        false, true, { selected.value = it }, {}, {})
                }
            }
        }
        compose.onNodeWithText("업무").assertIsSelected()
        capture("profiles")
        compose.onNodeWithText("개인").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("personal", selected.value?.name) }
        compose.onNodeWithText("리서치").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("research", selected.value?.name) }
    }

    @Test @Config(qualifiers = "ko-rKR-w720dp-h840dp-xhdpi")
    fun portraitFoldVoice() {
        voice("dark", "voice-fold")
        compose.onNodeWithTag(VOICE_FOCUS_SPLIT_LAYOUT_TEST_TAG).assertIsDisplayed()
    }

    @Test @Config(qualifiers = "ko-rKR-w320dp-h740dp-xhdpi")
    fun voiceLargeText() {
        RuntimeEnvironment.setFontScale(2f)
        voice("dark", "voice-large-text")
    }

    private fun voice(theme: String, name: String) {
        var microphoneRequests = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            HermesRelayTheme(themePreference = theme) {
                VoiceModeOverlay(
                    uiState = VoiceUiState(voiceMode = true),
                    onMicTap = { microphoneRequests++ }, onMicRelease = {}, onInterrupt = {},
                    onDismiss = {}, onModeChange = {}, onClearError = {}, voiceProfileName = "업무",
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)
        capture(name)
        compose.onNodeWithContentDescription(label(R.string.voice_overlay_tap_action_idle)).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(label(R.string.voice_overlay_exit_cd)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, microphoneRequests) }
    }

    @Test @Config(qualifiers = "ko-rKR-w320dp-h740dp-xhdpi")
    fun navigationLargeText() {
        RuntimeEnvironment.setFontScale(2f)
        val selected = mutableStateOf(ConnectDestination.Chat)
        compose.setContent {
            HermesRelayTheme(themePreference = "dark") {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        Spacer(Modifier.weight(1f))
                        ConnectNavigationBar(selected.value, { selected.value = it })
                    }
                }
            }
        }
        compose.onNodeWithText(label(R.string.screen_manage_label)).assertIsDisplayed().performClick()
        compose.onNodeWithText(label(R.string.screen_manage_label)).assertIsSelected()
        compose.onNodeWithText(label(R.string.screen_connections_label)).assertIsDisplayed().performClick()
        compose.onNodeWithText(label(R.string.screen_connections_label)).assertIsSelected()
        compose.onNodeWithText(label(R.string.screen_chat_label)).assertIsDisplayed().performClick()
        compose.onNodeWithText(label(R.string.screen_chat_label)).assertIsSelected()
        capture("navigation-large-text")
    }

    private fun label(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun capture(name: String) {
        val output = File("build/ui-evidence/connect-$name.png")
        output.parentFile?.mkdirs()
        compose.onRoot().captureRoboImage(output.absolutePath)
    }
}
