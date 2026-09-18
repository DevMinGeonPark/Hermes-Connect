package com.hermesandroid.relay.data

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.hermesandroid.relay.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class KoreanLocaleTest {
    private fun context(tag: String): Context {
        val application = RuntimeEnvironment.getApplication()
        val configuration = Configuration(application.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        return application.createConfigurationContext(configuration)
    }

    @Test
    fun koreanCatalogResolvesAcrossMainAndSupplementalFiles() {
        val ko = context("ko-KR")
        assertEquals("한국어", ko.getString(R.string.appearance_language_korean))
        assertEquals("새 대화", ko.getString(R.string.drawer_new_chat))
        assertEquals("사진.png 제거", ko.getString(R.string.pending_attachment_remove, "사진.png"))
        assertEquals("점프", ko.getString(R.string.pet_preview_jump))
        assertEquals("집중", ko.getString(R.string.voice_dock_focus))
        assertTrue(ko.getString(R.string.app_name).startsWith("Hermes-Connect"))
    }

    @Test
    fun koreanPluralAndReorderedArgumentsKeepTheirValues() {
        val ko = context("ko-KR")
        assertEquals("세션 3개", ko.resources.getQuantityString(R.plurals.drawer_project_session_count, 3, 3))
        assertEquals("서버 스냅샷: 전체 8개 중 2개 활성화", ko.getString(R.string.profile_inspector_skills_count_summary, 2, 8))
        assertEquals("응답 대기 2개 · 작업 중 3개", ko.getString(R.string.runtime_keepalive_counts, 2, 3))
    }

    @Test
    fun switchingLanguageDoesNotReplaceExistingCatalogs() {
        assertEquals("새 대화", context("ko").getString(R.string.drawer_new_chat))
        assertEquals("New Chat", context("en-US").getString(R.string.drawer_new_chat))
        listOf("de", "es", "ja", "pt-BR", "ru", "zh-Hans").forEach { tag ->
            assertTrue("Existing locale $tag must remain selectable", AppLanguage.fromLanguageTags(tag) != AppLanguage.SYSTEM_DEFAULT)
            assertTrue("Existing locale $tag must retain its translation", context(tag).getString(R.string.drawer_new_chat) != "New Chat")
        }
        assertEquals("새 대화", context("ko-KR").getString(R.string.drawer_new_chat))
    }
}
