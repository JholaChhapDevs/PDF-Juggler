package com.jholachhapdevs.pdfjuggler.feature.tts.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.jholachhapdevs.pdfjuggler.feature.tts.domain.*

/**
 * ViewModel for managing TTS functionality
 */
@Stable
class TTSViewModel(
    private val ttsService: TTSService,
    private val coroutineScope: CoroutineScope
) {
    // TTS state - using remember delegates to cache state
    var ttsState by mutableStateOf(TTSState.IDLE)
        private set
    var playbackInfo by mutableStateOf<TTSPlaybackInfo?>(null)
        private set
    var currentWordIndex by mutableStateOf(-1)
        private set
    var currentWords by mutableStateOf<List<String>>(emptyList())
        private set
    
    // UI state
    var selectedText by mutableStateOf<String?>(null)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Derived state
    val isPlaying: Boolean
        get() = playbackInfo?.isPlaying == true || ttsState == TTSState.SPEAKING
    
    // Prefer newly selected text over any stale playbackInfo
    val currentText: String?
        get() = selectedText?.takeIf { !it.isNullOrBlank() } ?: playbackInfo?.totalText

    init {
        // Initialize TTS service
        coroutineScope.launch {
            val result = ttsService.initialize()
            if (result is TTSResult.Error) {
                errorMessage = result.message
            }
        }
        
        // Collect state flows
        coroutineScope.launch {
            ttsService.state.collect { newState ->
                ttsState = newState
            }
        }
        
        coroutineScope.launch {
            ttsService.playbackInfo.collect { newInfo ->
                playbackInfo = newInfo
            }
        }
        
        // Collect word tracking if supported
        val tracker = ttsService as? WordTrackingTTS
        if (tracker != null) {
            coroutineScope.launch {
                tracker.currentWordIndex.collect { index: Int ->
                    currentWordIndex = index
                }
            }
        }
    }

    /**
     * Set text to be spoken
     */
    fun setTextToSpeak(text: String?) {
        selectedText = text
        currentWords = text?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
        currentWordIndex = -1
        errorMessage = null
    }

    /**
     * Start or resume TTS playback
     */
    fun play() {
        val textToSpeak = currentText
        if (textToSpeak.isNullOrBlank()) {
            errorMessage = "No text selected for TTS"
            return
        }

        // If we're paused but the newly selected text differs from the current playback text,
        // start a fresh speak() instead of resuming old audio
        val shouldStartFresh = playbackInfo?.totalText?.let { it != textToSpeak } ?: true

        coroutineScope.launch {
            val result = if (ttsState == TTSState.PAUSED && !shouldStartFresh) {
                ttsService.resume()
            } else {
                ttsService.speak(textToSpeak)
            }
            
            if (result is TTSResult.Error) {
                errorMessage = result.message
            } else {
                errorMessage = null
            }
        }
    }

    /**
     * Pause TTS playback
     */
    fun pause() {
        coroutineScope.launch {
            val result = ttsService.pause()
            if (result is TTSResult.Error) {
                errorMessage = result.message
            }
        }
    }

    /**
     * Stop TTS playback
     */
    fun stop() {
        coroutineScope.launch {
            val result = ttsService.stop()
            if (result is TTSResult.Error) {
                errorMessage = result.message
            } else {
                errorMessage = null
            }
        }
    }

    /**
     * Toggle between play and pause
     */
    fun togglePlayback() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
    }

    /**
     * Release TTS resources
     */
    fun release() {
        ttsService.release()
    }

    /**
     * Get available voices
     */
    fun getAvailableVoices(): List<String> {
        return ttsService.getAvailableVoices()
    }

    /**
     * Set TTS configuration
     */
    fun setConfig(config: TTSConfig) {
        coroutineScope.launch {
            val result = ttsService.setConfig(config)
            if (result is TTSResult.Error) {
                errorMessage = result.message
            }
        }
    }
}