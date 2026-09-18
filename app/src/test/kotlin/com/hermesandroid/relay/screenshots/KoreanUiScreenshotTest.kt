package com.hermesandroid.relay.screenshots

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hermesandroid.relay.R
import com.hermesandroid.relay.data.Attachment
import com.hermesandroid.relay.data.AttachmentState
import com.hermesandroid.relay.ui.components.CollapsibleAttachmentGroup
import com.hermesandroid.relay.ui.components.DestructiveVerbConfirmDialog
import com.hermesandroid.relay.ui.components.InboundAttachmentCard
import com.hermesandroid.relay.ui.components.InsecureConnectionAckDialog
import com.hermesandroid.relay.ui.theme.HermesRelayTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Offline evidence from production components; no ViewModels, media fetches, or server startup. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ko-rKR-w360dp-h800dp-xhdpi")
class KoreanUiScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private var evidenceName = "korean-ui"
    private var dialogFixture = false
    private var expectedFontScale = 1f

    @Test
    fun destructiveConfirmation_normalText() = destructiveConfirmation(1f, "100")

    @Test
    fun destructiveConfirmation_doubleText() = destructiveConfirmation(2f, "200")

    @Test
    fun insecureConnectionWarning_normalText() = insecureConnectionWarning(1f, "100")

    @Test
    fun insecureConnectionWarning_doubleText() = insecureConnectionWarning(2f, "200")

    @Test
    fun attachmentActions_normalText() = attachmentActions(1f, "100")

    @Test
    fun attachmentActions_doubleText() = attachmentActions(2f, "200")

    private fun destructiveConfirmation(scale: Float, suffix: String) {
        assertKoreanResources()
        evidenceName = "destructive-confirmation-$suffix"
        var denied = false
        var trustedApproval: Boolean? = null
        render(scale) {
            DestructiveVerbConfirmDialog(
                method = "/tap_text",
                verb = "삭제",
                fullText = "사진 3개 삭제",
                onAllow = { trustedApproval = it },
                onDeny = { denied = true },
            )
        }

        captureBeforeAssertions()

        assertReadableText("위험 작업 확인")
        bringIntoView("예상하지 못한 작업이라면 거부를 누르세요.")
        assertReadableText("예상하지 못한 작업이라면 거부를 누르세요.")
        assertReadableText("거부")
        assertReadableText("이 작업 허용")
        compose.onNodeWithText("이 작업 허용").assertHasClickAction()
        capture("destructive-confirmation-$suffix")

        // Translating the trust choice must not change its opt-in semantics.
        compose.onNodeWithText("거부").performClick()
        compose.runOnIdle {
            assertTrue(denied)
            assertNull(trustedApproval)
        }
        compose.onNodeWithText("이 작업 허용").performClick()
        compose.runOnIdle { assertEquals(false, trustedApproval) }
        val trustLabel = resources.getString(R.string.destructive_confirm_dont_ask, "삭제")
        bringIntoView(trustLabel)
        assertReadableText(trustLabel)
        compose.onNodeWithText(trustLabel).performClick()
        assertReadableText("거부")
        assertReadableText("이 작업 허용")
        compose.onNodeWithText("이 작업 허용").performClick()
        compose.runOnIdle { assertEquals(true, trustedApproval) }
    }

    private fun insecureConnectionWarning(scale: Float, suffix: String) {
        assertKoreanResources()
        evidenceName = "insecure-warning-$suffix"
        dialogFixture = true
        var confirmedReason: String? = null
        render(scale) {
            InsecureConnectionAckDialog(onConfirm = { confirmedReason = it }, onCancel = {})
        }

        captureBeforeAssertions()

        val warning = resources.getString(R.string.insecure_ack_body_1)
        assertTrue(warning.contains("누구나"))
        assertTrue(warning.contains("세션 토큰"))
        assertTrue(warning.contains("읽을 수 있습니다"))
        // A large paragraph can scroll, but its Text must never ellipsize or lose lines.
        bringIntoView(warning)
        compose.onNodeWithText(warning).assertIsDisplayed()
        assertUntruncatedText(warning)
        assertReadableText("취소")
        assertReadableText("이해했습니다")
        compose.onNodeWithText("이해했습니다").assertIsNotEnabled()

        val reason = "로컬 개발 전용"
        bringIntoView(reason)
        assertReadableText(reason)
        compose.onNodeWithText(reason).performClick()
        assertReadableText("취소")
        assertReadableText("이해했습니다")
        compose.onNodeWithText("이해했습니다").assertIsEnabled()
        capture("insecure-warning-$suffix", dialog = true)
        compose.onNodeWithText("이해했습니다").performClick()
        compose.runOnIdle { assertEquals("local_dev", confirmedReason) }
    }

    private fun attachmentActions(scale: Float, suffix: String) {
        assertKoreanResources()
        evidenceName = "attachment-actions-$suffix"
        var retries = 0
        // FAILED renders local UI only: no URI, media loader, download, or network client.
        val attachment = Attachment(
            contentType = "application/pdf",
            content = "",
            fileName = "작업 결과.pdf",
            state = AttachmentState.FAILED,
        )
        render(scale) {
            Box(Modifier.padding(16.dp)) {
                CollapsibleAttachmentGroup("korean-offline", listOf(attachment)) {
                    InboundAttachmentCard(
                        attachment = attachment,
                        onRetry = { retries++ },
                        onManualFetch = { error("A failed attachment must not request a download") },
                    )
                }
            }
        }

        captureBeforeAssertions()

        assertReadableText("첨부파일 1개")
        assertReadableText("첨부파일 오류")
        assertReadableText("눌러서 다시 시도")
        compose.onNodeWithContentDescription("첨부파일 접기")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "펼쳐짐"))
        capture("attachment-actions-$suffix")
        compose.onNodeWithText("눌러서 다시 시도").performClick()
        compose.runOnIdle { assertEquals(1, retries) }

        compose.onNodeWithContentDescription("첨부파일 접기").performClick()
        compose.onNodeWithContentDescription("첨부파일 펼치기")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "접힘"))
            .performClick()
        assertReadableText("눌러서 다시 시도")
    }

    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    private fun assertKoreanResources() {
        assertEquals("ko", resources.configuration.locales[0].language)
        assertEquals("거부", resources.getString(R.string.destructive_confirm_deny))
        assertEquals("첨부파일 접기", resources.getString(R.string.attachment_group_collapse))
    }

    private fun render(scale: Float, content: @Composable () -> Unit) {
        expectedFontScale = scale
        // Dialog windows derive density from Android configuration, not necessarily
        // the parent's CompositionLocalProvider. Exercise the same system scale there.
        RuntimeEnvironment.setFontScale(scale)
        assertEquals("System font scale must match the fixture", scale, resources.configuration.fontScale, 0.001f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                HermesRelayTheme(themePreference = "dark") {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        content()
                    }
                }
            }
        }
    }

    private fun bringIntoView(text: String) {
        // Fitting dialogs need no scrolling; overflowing dialogs must expose a real scroll parent.
        try {
            compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
        } catch (_: AssertionError) {
            // The following visibility/layout assertions still fail if it is actually clipped.
        }
    }

    private fun assertUntruncatedText(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val diagnostics = textLayoutDiagnostic(text, layout)
        val failures = mutableListOf<String>()
        if (kotlin.math.abs(layout.layoutInput.density.fontScale - expectedFontScale) > 0.001f) {
            failures += "Actual text fontScale must be $expectedFontScale, was ${layout.layoutInput.density.fontScale}"
        }
        if (layout.multiParagraph.didExceedMaxLines) {
            failures += "Text exceeded maxLines"
        }
        if (layout.lineCount == 0 || layout.getLineEnd(layout.lineCount - 1) < text.length) {
            failures += "Layout did not include every character"
        }

        // Compose can retain the available paragraph width even when the Text node
        // wraps its content. Compare the actual line/glyph extents with the measured
        // node; paragraph.width alone incorrectly flags short, fully visible labels.
        fun checkExtent(label: String, left: Float, top: Float, right: Float, bottom: Float) {
            val roundingTolerancePx = 1f
            if (left < -roundingTolerancePx || top < -roundingTolerancePx ||
                right > layout.size.width + roundingTolerancePx ||
                bottom > layout.size.height + roundingTolerancePx
            ) {
                failures += "$label extends outside measured ${layout.size}: [$left, $top, $right, $bottom]"
            }
        }
        repeat(layout.lineCount) { line ->
            if (layout.isLineEllipsized(line)) failures += "Line $line was ellipsized"
            checkExtent(
                "Line $line",
                layout.getLineLeft(line),
                layout.getLineTop(line),
                layout.getLineRight(line),
                layout.getLineBottom(line),
            )
        }
        text.forEachIndexed { index, character ->
            if (!character.isWhitespace()) {
                val bounds = layout.getBoundingBox(index)
                checkExtent("Glyph $index <$character>", bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
        }
        if (failures.isNotEmpty()) {
            recordFailure(failures.joinToString("\n") + "\n" + diagnostics)
        }
        assertTrue("Text must be fully laid out: $text\n${failures.joinToString("\n")}\n$diagnostics", failures.isEmpty())
        return layout
    }

    private fun assertReadableText(text: String) {
        val node = compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
        val layout = assertUntruncatedText(text)
        val visible = node.fetchSemanticsNode().boundsInRoot
        val diagnostics = textLayoutDiagnostic(text, layout)
        if (visible.height + 1f < layout.size.height || visible.width + 1f < layout.size.width) {
            recordFailure(diagnostics)
        }
        assertTrue("Full action text height must be visible: $text\n$diagnostics", visible.height + 1f >= layout.size.height)
        assertTrue("Full action text width must be visible: $text\n$diagnostics", visible.width + 1f >= layout.size.width)
    }

    private fun textLayoutDiagnostic(text: String, layout: TextLayoutResult): String = buildString {
        val input = layout.layoutInput
        val bounds = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        appendLine("fixture=$evidenceName text=<$text>")
        appendLine("viewport=${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
        appendLine("locale=${resources.configuration.locales} density=${input.density.density} fontScale=${input.density.fontScale} expectedFontScale=$expectedFontScale systemFontScale=${resources.configuration.fontScale}")
        appendLine("size=${layout.size} constraints=${input.constraints} semanticsBounds=$bounds")
        appendLine("didOverflowHeight=${layout.didOverflowHeight} didOverflowWidth=${layout.didOverflowWidth} hasVisualOverflow=${layout.hasVisualOverflow}")
        appendLine("paragraphSize=${layout.multiParagraph.width}x${layout.multiParagraph.height} didExceedMaxLines=${layout.multiParagraph.didExceedMaxLines}")
        appendLine("softWrap=${input.softWrap} maxLines=${input.maxLines} overflow=${input.overflow}")
        appendLine("style=${input.style}")
        repeat(layout.lineCount) { line ->
            appendLine(
                "line[$line] start=${layout.getLineStart(line)} end=${layout.getLineEnd(line)} " +
                    "visibleEnd=${layout.getLineEnd(line, visibleEnd = true)} " +
                    "left=${layout.getLineLeft(line)} right=${layout.getLineRight(line)} " +
                    "top=${layout.getLineTop(line)} bottom=${layout.getLineBottom(line)} " +
                    "baseline=${layout.getLineBaseline(line)} ellipsized=${layout.isLineEllipsized(line)}",
            )
        }
        text.forEachIndexed { index, character ->
            if (!character.isWhitespace()) {
                appendLine("glyph[$index] <$character> bounds=${layout.getBoundingBox(index)}")
            }
        }
    }

    private fun recordFailure(diagnostics: String) {
        val directory = File("build/ui-evidence/korean-locale").apply { mkdirs() }
        File(directory, "$evidenceName-layout-failure.txt").appendText(diagnostics + "\n")
        System.err.println(diagnostics)
        // Screenshot errors must not hide the layout assertion and its measured evidence.
        runCatching { capture("$evidenceName-at-failure", dialogFixture) }
            .onFailure { System.err.println("Failure screenshot could not be captured: $it") }
    }

    private fun captureBeforeAssertions() {
        File("build/ui-evidence/korean-locale/$evidenceName-layout-failure.txt").delete()
        runCatching { capture("$evidenceName-before-assertions", dialogFixture) }
            .onFailure { System.err.println("Initial screenshot could not be captured: $it") }
    }

    private fun capture(name: String, dialog: Boolean = false) {
        val directory = File("build/ui-evidence/korean-locale").apply { mkdirs() }
        val node = if (dialog) compose.onNode(isDialog()) else compose.onRoot()
        node.captureRoboImage(File(directory, "$name.png").path)
    }
}
