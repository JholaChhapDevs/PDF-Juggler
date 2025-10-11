package com.jholachhapdevs.pdfjuggler.feature.tts.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Text-to-Speech functionality
 */
interface TTSService {
    
    /**
     * Observable state of the TTS engine
     */
    val state: StateFlow<TTSState>
    
    /**
     * Observable playback information
     */
    val playbackInfo: StateFlow<TTSPlaybackInfo?>
    
    /**
     * Initialize the TTS engine
     * @return Result indicating success or failure
     */
    suspend fun initialize(): TTSResult
    
    /**
     * Speak the given text
     * @param text The text to be spoken
     * @param config Configuration for speech synthesis
     * @return Result indicating success or failure
     */
    suspend fun speak(text: String, config: TTSConfig = TTSConfig()): TTSResult
    
    /**
     * Pause current speech
     * @return Result indicating success or failure
     */
    suspend fun pause(): TTSResult
    
    /**
     * Resume paused speech
     * @return Result indicating success or failure
     */
    suspend fun resume(): TTSResult
    
    /**
     * Stop current speech
     * @return Result indicating success or failure
     */
    suspend fun stop(): TTSResult
    
    /**
     * Check if TTS is currently speaking
     * @return True if speaking, false otherwise
     */
    fun isSpeaking(): Boolean
    
    /**
     * Get available voices
     * @return List of available voice identifiers
     */
    fun getAvailableVoices(): List<String>
    
    /**
     * Set speech configuration
     * @param config New configuration to apply
     * @return Result indicating success or failure
     */
    suspend fun setConfig(config: TTSConfig): TTSResult
    
    /**
     * Release TTS resources
     */
    fun release()
}