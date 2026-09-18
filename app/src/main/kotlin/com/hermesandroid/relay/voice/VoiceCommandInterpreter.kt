package com.hermesandroid.relay.voice

import java.util.Locale

/**
 * Local actions that can be requested from a committed voice transcript.
 *
 * These are deliberately separate from normal Hermes prompts. Callers must
 * invoke [VoiceCommandInterpreter.interpretFinalTranscript] only after STT (or
 * a realtime provider) has emitted a final transcript; partial transcripts are
 * never safe command boundaries.
 */
internal enum class VoiceCommandAction {
    EndVoiceChat,
    StopResponse,
    CancelBackgroundTask,
    PauseContinuousListening,
    ResumeContinuousListening,
    RepeatBackgroundAnswer,
    StartNewChat,
}

/** State gates that keep an exact command phrase from becoming a global hotword. */
internal data class VoiceCommandContext(
    val voiceChatActive: Boolean = false,
    val stopPhrases: List<String> = listOf("stop"),
    val responseActive: Boolean = false,
    val interruptedActiveResponse: Boolean = false,
    val backgroundTaskActive: Boolean = false,
    val backgroundAnswerAvailable: Boolean = false,
    val continuousModeSelected: Boolean = false,
    val continuousListeningActive: Boolean = false,
    val continuousListeningPaused: Boolean = false,
    val canStartNewChat: Boolean = false,
)

/**
 * Conservative, exact-only interpreter for hands-free Voice controls.
 *
 * False negatives are preferred: a phrase must match one complete normalized
 * utterance and its corresponding state gate. There is no prefix, substring,
 * edit-distance, or fuzzy matching, so ordinary prompts such as "How do I stop
 * talking too quickly?" continue to Hermes unchanged.
 */
internal object VoiceCommandInterpreter {
    private val stopResponsePhrases = setOf(
        "stop speaking",
        "stop talking",
        "stop the response",
        "stop your response",
        "말하기 중지",
        "응답 중지",
        "그만 말해",
    )
    private val cancelBackgroundTaskPhrases = setOf(
        "cancel the background task",
        "cancel my background task",
        "cancel that background task",
        "cancel the running background task",
        "백그라운드 작업 취소",
        "백그라운드 작업 취소해",
        "백그라운드 작업을 취소해",
    )
    private val pauseContinuousPhrases = setOf(
        "pause",
        "pause continuous listening",
        "pause hands free listening",
        "일시 중지",
        "계속 듣기 일시 중지",
    )
    private val resumeContinuousPhrases = setOf(
        "resume",
        "resume continuous listening",
        "resume hands free listening",
        "재개",
        "계속 듣기 재개",
    )
    private val repeatBackgroundAnswerPhrases = setOf(
        "repeat that",
        "repeat the background answer",
        "repeat the last background answer",
        "repeat that background answer",
        "다시 말해",
        "다시 말해 줘",
        "마지막 백그라운드 답변 다시 말해",
    )
    private val newChatPhrases = setOf(
        "new chat",
        "start a new chat",
        "open a new chat",
        "create a new chat",
        "새 대화",
        "새 대화 시작",
        "새 대화 시작해",
    )

    fun interpretFinalTranscript(
        rawTranscript: String,
        context: VoiceCommandContext,
    ): VoiceCommandAction? {
        val phrase = normalize(rawTranscript)
        if (phrase.isEmpty()) return null

        return when {
            context.backgroundTaskActive && phrase in cancelBackgroundTaskPhrases ->
                VoiceCommandAction.CancelBackgroundTask
            context.voiceChatActive && phrase in context.stopPhrases.map(::normalize) ->
                VoiceCommandAction.EndVoiceChat
            context.responseActive &&
                phrase in stopResponsePhrases ->
                VoiceCommandAction.StopResponse
            context.continuousModeSelected &&
                context.interruptedActiveResponse &&
                phrase in pauseContinuousPhrases -> VoiceCommandAction.PauseContinuousListening
            context.continuousModeSelected &&
                context.continuousListeningActive &&
                phrase in pauseContinuousPhrases -> VoiceCommandAction.PauseContinuousListening
            context.continuousModeSelected &&
                context.continuousListeningPaused &&
                phrase in resumeContinuousPhrases -> VoiceCommandAction.ResumeContinuousListening
            context.backgroundAnswerAvailable && phrase in repeatBackgroundAnswerPhrases ->
                VoiceCommandAction.RepeatBackgroundAnswer
            context.canStartNewChat && phrase in newChatPhrases -> VoiceCommandAction.StartNewChat
            else -> null
        }
    }

    private fun normalize(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\p{Punct}\\p{P}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
