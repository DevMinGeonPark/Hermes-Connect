package com.hermesandroid.relay.voice

import android.app.Application
import com.hermesandroid.relay.viewmodel.VoiceViewModel
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceConfirmationCancelTest {
    private val viewModel = VoiceViewModel(mockk<Application>(relaxed = true))

    @Test
    fun `Korean confirmation cancellation accepts only complete allowed utterances`() {
        listOf("취소", "취소해", "취소해 줘", "취소해 주세요", "  취소!  ").forEach {
            assertTrue(it, viewModel.isCancelUtterance(it))
        }
        listOf("취소하지 마", "취소 방법 알려줘", "취소해 주세요가 무슨 뜻이야", "예약 취소", "네", "").forEach {
            assertFalse(it, viewModel.isCancelUtterance(it))
        }
    }

    @Test
    fun `existing English confirmation cancellation remains supported`() {
        listOf("cancel", "Cancel please!", "never mind", "no don't", "wait").forEach {
            assertTrue(it, viewModel.isCancelUtterance(it))
        }
        listOf("yes", "ok", "explain how to cancel", "cancellation").forEach {
            assertFalse(it, viewModel.isCancelUtterance(it))
        }
    }
}
