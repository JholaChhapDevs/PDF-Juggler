package com.jholachhapdevs.pdfjuggler.feature.tts.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Optional capability interface for TTS services that can report the
 * index of the currently spoken word in the provided text.
 */
interface WordTrackingTTS {
    /**
     * Emits the currently spoken word index (0-based). Emits -1 when idle or unknown.
     */
    val currentWordIndex: StateFlow<Int>
}

