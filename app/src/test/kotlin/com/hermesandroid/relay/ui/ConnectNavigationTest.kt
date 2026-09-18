package com.hermesandroid.relay.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hermesandroid.relay.ui.components.ConnectDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h840dp-xhdpi")
class ConnectNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tabsReturnToChatAndPreserveDraft() {
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = Screen.Chat.route) {
                composable(Screen.Chat.route, arguments = listOf(
                    navArgument(Screen.Chat.ARG_OPEN_AGENT_SHEET) { type = NavType.BoolType; defaultValue = false },
                    navArgument(Screen.Chat.ARG_SESSION_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.Chat.ARG_PROFILE) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.Chat.ARG_PROACTIVE_CHAT_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                )) {
                    var draft by rememberSaveable { mutableStateOf("") }
                    BasicTextField(draft, { draft = it }, Modifier.testTag("draft"))
                }
                composable(Screen.Manage.route) { Text("Manage") }
                composable(Screen.ConnectionsSettings.route) { Text("Connections") }
            }
        }
        compose.onNodeWithTag("draft").performTextInput("Unsent draft")
        repeat(2) {
            listOf(ConnectDestination.Manage, ConnectDestination.Connections, ConnectDestination.Chat).forEach { tab ->
                compose.runOnIdle { controller.openConnectDestination(tab) }
                compose.waitForIdle()
                compose.runOnIdle {
                    val expected = when (tab) {
                        ConnectDestination.Chat -> Screen.Chat.route
                        ConnectDestination.Manage -> Screen.Manage.route
                        ConnectDestination.Connections -> Screen.ConnectionsSettings.route
                    }
                    assertEquals(expected, controller.currentDestination?.route)
                }
            }
            compose.onNodeWithTag("draft").assertTextEquals("Unsent draft")
        }
    }
}
