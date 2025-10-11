package com.jholachhapdevs.pdfjuggler.feature.tts.domain

/**
 * Represents the current state of the TTS engine
 */
enum class TTSState {
    IDLE,
    SPEAKING,
    PAUSED,
    STOPPED,
    ERROR
}

/**
 * Represents TTS configuration options
 */
data class TTSConfig(
    val speed: Float = 1.0f,                    // Speech speed multiplier (0.5 - 2.0)
    val pitch: Float = 1.0f,                    // Voice pitch multiplier (0.5 - 2.0)  
    val volume: Float = 1.0f,                   // Volume level (0.0 - 1.0)
    val voice: String = "cmu-slt-hsmm",         // Voice identifier (MaryTTS voice name)
    val languageCode: String = "en-US",         // Language code (e.g., "en-US", "fr-FR")
    val gender: VoiceGender = VoiceGender.FEMALE, // Voice gender preference
    val audioEncoding: AudioEncoding = AudioEncoding.LINEAR16, // Audio format
    val useSSML: Boolean = false                 // Whether to interpret input as SSML
)

/**
 * Represents the result of a TTS operation
 */
sealed class TTSResult {
    object Success : TTSResult()
    data class Error(val message: String, val exception: Throwable? = null) : TTSResult()
}

/**
 * Voice gender options for Google Cloud TTS
 */
enum class VoiceGender {
    MALE,
    FEMALE,
    NEUTRAL
}

/**
 * Audio encoding options for Google Cloud TTS
 */
enum class AudioEncoding {
    LINEAR16,
    MP3,
    OGG_OPUS,
    MULAW,
    ALAW
}

/**
 * Represents available Google Cloud TTS voices
 */
data class TTSVoice(
    val name: String,
    val languageCode: String,
    val gender: VoiceGender,
    val naturalSampleRateHertz: Int
)

/**
 * Represents TTS playback information
 */
data class TTSPlaybackInfo(
    val totalText: String,
    val currentPosition: Int = 0,
    val isPlaying: Boolean = false,
    val config: TTSConfig = TTSConfig()
)
