package com.jholachhapdevs.pdfjuggler.feature.tts

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import com.jholachhapdevs.pdfjuggler.feature.tts.domain.TTSService
import com.jholachhapdevs.pdfjuggler.feature.tts.ui.TTSViewModel

/**
 * TTS service provider for creating and managing TTS instances
 */
object TTSServiceProvider {
    
    /**
     * Create a TTS service instance
     */
    fun createTTSService(): TTSService {
        return MaryTTSService()
    }
}

/**
 * Composable function to create and remember a TTS view model
 */
@Composable
fun rememberTTSViewModel(
    ttsService: TTSService = remember { TTSServiceProvider.createTTSService() }
): TTSViewModel {
    val coroutineScope = rememberCoroutineScope()
    return remember(ttsService) { 
        TTSViewModel(ttsService, coroutineScope) 
    }
}