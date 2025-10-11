package com.jholachhapdevs.pdfjuggler.feature.tts

import com.jholachhapdevs.pdfjuggler.feature.tts.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean
import java.io.ByteArrayInputStream
import javax.sound.sampled.*
import marytts.LocalMaryInterface
import marytts.modules.synthesis.Voice

/**
 * MaryTTS implementation of the TTS service
 */
class MaryTTSService : TTSService, WordTrackingTTS {

    private var maryTTS: LocalMaryInterface? = null
    private val isInitialized = AtomicBoolean(false)
    private var currentConfig = TTSConfig()
    private val isSpeaking = AtomicBoolean(false)
    private var speechJob: Job? = null
    private var audioPlayJob: Job? = null
    private var currentWords: List<String> = emptyList()
    private val _currentWordIndex = MutableStateFlow(-1)
    override val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()

    private var audioClip: Clip? = null
    private var currentAudioData: ByteArray? = null
    private var isPaused = false
    private var pausedPosition: Long = 0

    private val _state = MutableStateFlow(TTSState.IDLE)
    override val state: StateFlow<TTSState> = _state.asStateFlow()

    private val _playbackInfo = MutableStateFlow<TTSPlaybackInfo?>(null)
    override val playbackInfo: StateFlow<TTSPlaybackInfo?> = _playbackInfo.asStateFlow()

    override suspend fun initialize(): TTSResult = withContext(Dispatchers.IO) {
        try {
            if (isInitialized.get()) {
                return@withContext TTSResult.Success
            }

            _state.value = TTSState.IDLE

            // Initialize MaryTTS
            maryTTS = LocalMaryInterface()

            println("DEBUG: MaryTTS initialized successfully")

            isInitialized.set(true)
            TTSResult.Success
        } catch (e: Exception) {
            _state.value = TTSState.ERROR
            TTSResult.Error("Failed to initialize MaryTTS: ${e.message}", e)
        }
    }

    override suspend fun speak(text: String, config: TTSConfig): TTSResult = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: speak() called with text: '$text' (${text.length} characters)")
            val startTime = System.currentTimeMillis()

            if (!isInitialized.get()) {
                val initResult = initialize()
                if (initResult is TTSResult.Error) {
                    return@withContext initResult
                }
            }

            val mary = maryTTS ?: return@withContext TTSResult.Error("MaryTTS not available")

            // Stop any current playback
            stop()

            // Split text into words for tracking
            currentWords = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
            _currentWordIndex.value = -1

            println("DEBUG: Split into ${currentWords.size} words")

            _state.value = TTSState.SPEAKING
            isSpeaking.set(true)
            _playbackInfo.value = TTSPlaybackInfo(
                totalText = text,
                currentPosition = 0,
                isPlaying = true,
                config = config
            )

            // Apply configuration
            currentConfig = config
            applyConfigToMary(mary, config)

            // Generate audio - use generateAudio which returns AudioInputStream
            println("DEBUG: Calling mary.generateAudio() with text length: ${text.length}")
            val audioGenerationStart = System.currentTimeMillis()
            val audioInputStream = mary.generateAudio(text)
            val audioGenerationTime = System.currentTimeMillis() - audioGenerationStart
            println("DEBUG: Audio generation took ${audioGenerationTime}ms")
            println("DEBUG: Audio stream created, available bytes: ${audioInputStream.available()}")

            // Convert to AudioInputStream with proper format
            val audioFormat = audioInputStream.format
            println("DEBUG: Audio format from MaryTTS: $audioFormat")

            // Use streaming approach for faster playback start
            // Start playback immediately and read audio data in parallel
            val audioReadingStart = System.currentTimeMillis()

            // Start word tracking job immediately
            speechJob = launch {
                // Add a smaller delay to account for audio buffering
                delay(50) // Reduced from 200ms to 50ms
                startWordTracking()
            }

            // Start streaming playback
            audioPlayJob = launch {
                playAudioStreaming(audioInputStream, audioFormat)
            }

            val totalSetupTime = System.currentTimeMillis() - startTime
            println("DEBUG: Total setup time: ${totalSetupTime}ms (generation: ${audioGenerationTime}ms)")

            // Wait for audio playback to complete
            audioPlayJob?.join()

            // Cancel word tracking
            speechJob?.cancel()
            _currentWordIndex.value = -1

            isSpeaking.set(false)

            if (_state.value == TTSState.SPEAKING) {
                _state.value = TTSState.IDLE
            }

            _playbackInfo.value = _playbackInfo.value?.copy(isPlaying = false)

            TTSResult.Success
        } catch (e: Exception) {
            speechJob?.cancel()
            audioPlayJob?.cancel()
            _currentWordIndex.value = -1
            _state.value = TTSState.ERROR
            isSpeaking.set(false)
            _playbackInfo.value = _playbackInfo.value?.copy(isPlaying = false)
            TTSResult.Error("Failed to speak text: ${e.message}", e)
        }
    }

    private fun applyConfigToMary(mary: LocalMaryInterface, config: TTSConfig) {
        // Set voice based on gender preference
        val availableVoices = Voice.getAvailableVoices()
        if (availableVoices.isNotEmpty()) {
            // Try to find a voice matching the gender preference
            val preferredVoice = when (config.gender) {
                VoiceGender.MALE -> availableVoices.find { voice ->
                    voice.getName().contains("male", ignoreCase = true)
                }
                VoiceGender.FEMALE -> availableVoices.find { voice ->
                    voice.getName().contains("female", ignoreCase = true) ||
                    voice.getName().contains("slt", ignoreCase = true)
                }
                VoiceGender.NEUTRAL -> availableVoices.firstOrNull()
            }
            mary.voice = (preferredVoice ?: availableVoices.first()).getName()
        }

        // MaryTTS doesn't have built-in speed/pitch control like Google Cloud TTS
        // These would need to be applied via audio processing if needed
    }

    override suspend fun pause(): TTSResult = withContext(Dispatchers.IO) {
        try {
            audioClip?.let { clip ->
                if (clip.isRunning) {
                    pausedPosition = clip.microsecondPosition
                    clip.stop()
                    isPaused = true
                }
            }

            // Pause word tracking
            speechJob?.cancel()
            speechJob = null

            _state.value = TTSState.PAUSED
            isSpeaking.set(false)
            _playbackInfo.value = _playbackInfo.value?.copy(isPlaying = false)
            TTSResult.Success
        } catch (e: Exception) {
            TTSResult.Error("Failed to pause: ${e.message}", e)
        }
    }

    override suspend fun resume(): TTSResult = withContext(Dispatchers.IO) {
        try {
            if (isPaused && currentAudioData != null) {
                _state.value = TTSState.SPEAKING
                isSpeaking.set(true)
                _playbackInfo.value = _playbackInfo.value?.copy(isPlaying = true)

                // Resume word tracking from current position
                speechJob = launch {
                    startWordTrackingFromPosition()
                }

                // Resume audio playback
                audioPlayJob = launch {
                    resumeAudio()
                }

                isPaused = false
                TTSResult.Success
            } else {
                // If no paused state, restart from beginning
                val currentText = _playbackInfo.value?.totalText
                if (!currentText.isNullOrBlank()) {
                    speak(currentText, currentConfig)
                } else {
                    _state.value = TTSState.IDLE
                    TTSResult.Success
                }
            }
        } catch (e: Exception) {
            TTSResult.Error("Failed to resume: ${e.message}", e)
        }
    }

    private suspend fun startWordTracking() {
        // Small initial delay to sync with audio start
        delay(200)

        // Calculate timing based on speech rate
        val baseWordsPerMinute = 150f * currentConfig.speed // MaryTTS typical speed
        val millisecondsPerWord = (60000f / baseWordsPerMinute).toLong()

        for (i in currentWords.indices) {
            if (!isSpeaking.get() || _state.value != TTSState.SPEAKING || !currentCoroutineContext().isActive) break

            _currentWordIndex.value = i

            // Dynamic timing based on word characteristics
            val currentWord = currentWords[i]
            val wordDelay = when {
                currentWord.length > 8 -> (millisecondsPerWord * 1.2).toLong()
                currentWord.endsWith(".") || currentWord.endsWith("!") || currentWord.endsWith("?") ->
                    (millisecondsPerWord * 1.5).toLong()
                currentWord.endsWith(",") || currentWord.endsWith(";") ->
                    (millisecondsPerWord * 1.3).toLong()
                else -> millisecondsPerWord
            }

            delay(wordDelay)
        }

        _currentWordIndex.value = -1
    }

    private suspend fun startWordTrackingFromPosition() {
        // Calculate approximate word position based on pause position
        val totalDuration = audioClip?.microsecondLength ?: 0L
        val progressRatio = if (totalDuration > 0) pausedPosition.toFloat() / totalDuration else 0f
        val startWordIndex = (currentWords.size * progressRatio).toInt().coerceIn(0, currentWords.size - 1)

        delay(100) // Small sync delay

        val baseWordsPerMinute = 150f * currentConfig.speed
        val millisecondsPerWord = (60000f / baseWordsPerMinute).toLong()

        for (i in startWordIndex until currentWords.size) {
            if (!isSpeaking.get() || _state.value != TTSState.SPEAKING || !currentCoroutineContext().isActive) break

            _currentWordIndex.value = i

            val currentWord = currentWords[i]
            val wordDelay = when {
                currentWord.length > 8 -> (millisecondsPerWord * 1.2).toLong()
                currentWord.endsWith(".") || currentWord.endsWith("!") || currentWord.endsWith("?") ->
                    (millisecondsPerWord * 1.5).toLong()
                currentWord.endsWith(",") || currentWord.endsWith(";") ->
                    (millisecondsPerWord * 1.3).toLong()
                else -> millisecondsPerWord
            }

            delay(wordDelay)
        }

        _currentWordIndex.value = -1
    }

    private suspend fun playAudioStreaming(audioInputStream: AudioInputStream, audioFormat: AudioFormat) = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: playAudioStreaming called")
            println("DEBUG: Audio format: $audioFormat")
            val playbackStart = System.currentTimeMillis()

            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)

            // Check if the line is supported
            if (!AudioSystem.isLineSupported(info)) {
                println("DEBUG: SourceDataLine not supported, falling back to Clip")
                return@withContext playAudioFromStream(audioInputStream, audioFormat)
            }

            // Get a SourceDataLine for streaming playback
            val line = try {
                AudioSystem.getLine(info) as SourceDataLine
            } catch (e: LineUnavailableException) {
                println("DEBUG: Default SourceDataLine unavailable, trying with specific mixer...")
                val mixers = AudioSystem.getMixerInfo()
                var sourceLine: SourceDataLine? = null
                for (mixerInfo in mixers) {
                    try {
                        val mixer = AudioSystem.getMixer(mixerInfo)
                        if (mixer.isLineSupported(info)) {
                            sourceLine = mixer.getLine(info) as SourceDataLine
                            println("DEBUG: Using mixer for streaming: ${mixerInfo.name}")
                            break
                        }
                    } catch (ex: Exception) {
                        // Try next mixer
                        continue
                    }
                }
                sourceLine ?: throw e
            }

            line.open(audioFormat)
            line.start()
            println("DEBUG: SourceDataLine started, setup took ${System.currentTimeMillis() - playbackStart}ms")

            // Stream audio data in chunks for immediate playback
            val buffer = ByteArray(4096) // Smaller buffer for lower latency
            var bytesRead: Int
            var totalBytesWritten = 0
            val audioStart = System.currentTimeMillis()

            try {
                while (audioInputStream.read(buffer).also { bytesRead = it } != -1 &&
                       isSpeaking.get() && _state.value == TTSState.SPEAKING) {

                    // Write audio data immediately to the line
                    var offset = 0
                    while (offset < bytesRead) {
                        val written = line.write(buffer, offset, bytesRead - offset)
                        offset += written
                        totalBytesWritten += written
                    }

                    // Log progress occasionally
                    if (totalBytesWritten % 32768 == 0) {
                        val elapsed = System.currentTimeMillis() - audioStart
                        println("DEBUG: Streamed ${totalBytesWritten} bytes in ${elapsed}ms")
                    }
                }
            } finally {
                // Ensure all buffered data is played
                line.drain()
                line.stop()
                line.close()
                audioInputStream.close()
            }

            val totalTime = System.currentTimeMillis() - playbackStart
            println("DEBUG: Streaming playback completed in ${totalTime}ms, total bytes: $totalBytesWritten")

        } catch (e: Exception) {
            println("DEBUG: Exception in playAudioStreaming: ${e.message}")
            e.printStackTrace()
            // Fallback to regular playback
            audioInputStream.reset()
            playAudioFromStream(audioInputStream, audioFormat)
        }
    }

    private suspend fun playAudioFromStream(audioInputStream: AudioInputStream, audioFormat: AudioFormat) = withContext(Dispatchers.IO) {
        // Read all audio data first (fallback method)
        val outputStream = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var bytesRead: Int
        var totalBytesRead = 0

        println("DEBUG: Reading full audio stream for fallback playback...")
        try {
            while (audioInputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }
        } catch (e: Exception) {
            println("DEBUG: Exception while reading audio stream: ${e.message}")
            throw e
        }

        val audioBytes = outputStream.toByteArray()
        println("DEBUG: Read ${audioBytes.size} bytes for fallback playback")

        if (audioBytes.isNotEmpty()) {
            currentAudioData = audioBytes
            playAudio(audioBytes, audioFormat)
        }
    }

    private suspend fun playAudio(audioData: ByteArray, audioFormat: AudioFormat) = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: playAudio called with ${audioData.size} bytes")
            println("DEBUG: Audio format: $audioFormat")

            val audioInputStream = AudioInputStream(
                ByteArrayInputStream(audioData),
                audioFormat,
                audioData.size / audioFormat.frameSize.toLong()
            )

            val info = DataLine.Info(Clip::class.java, audioFormat)

            // Check if the line is supported
            if (!AudioSystem.isLineSupported(info)) {
                println("DEBUG: Audio line not supported for format: $audioFormat")
                // Try to find a supported format by getting available mixers
                val mixers = AudioSystem.getMixerInfo()
                println("DEBUG: Available audio mixers:")
                mixers.forEach { mixer ->
                    println("  - ${mixer.name}: ${mixer.description}")
                }
                throw UnsupportedAudioFileException("Audio format not supported: $audioFormat")
            }

            audioClip?.close()
            
            // Try to get the line with explicit mixer if needed
            audioClip = try {
                AudioSystem.getLine(info) as Clip
            } catch (e: LineUnavailableException) {
                println("DEBUG: Default line unavailable, trying with specific mixer...")
                val mixers = AudioSystem.getMixerInfo()
                var clip: Clip? = null
                for (mixerInfo in mixers) {
                    try {
                        val mixer = AudioSystem.getMixer(mixerInfo)
                        if (mixer.isLineSupported(info)) {
                            clip = mixer.getLine(info) as Clip
                            println("DEBUG: Using mixer: ${mixerInfo.name}")
                            break
                        }
                    } catch (ex: Exception) {
                        // Try next mixer
                        continue
                    }
                }
                clip ?: throw e
            }
            
            audioClip?.let { clip ->
                clip.open(audioInputStream)
                println("DEBUG: Clip opened, duration: ${clip.microsecondLength / 1000000.0} seconds")
                println("DEBUG: Clip frame length: ${clip.frameLength}")

                // Add a line listener to track playback events
                clip.addLineListener { event ->
                    println("DEBUG: Line event: ${event.type}")
                    when (event.type) {
                        LineEvent.Type.START -> println("DEBUG: Audio started playing")
                        LineEvent.Type.STOP -> println("DEBUG: Audio stopped playing")
                        LineEvent.Type.CLOSE -> println("DEBUG: Audio line closed")
                    }
                }

                // Ensure the clip has control over volume (Windows issue workaround)
                val volumeControl = try {
                    clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
                } catch (e: Exception) {
                    println("DEBUG: No volume control available")
                    null
                }

                volumeControl?.let { control ->
                    val currentVolume = control.value
                    println("DEBUG: Current volume: $currentVolume dB")
                    // Ensure volume is not muted
                    if (currentVolume < control.minimum) {
                        control.value = control.minimum + 1.0f
                        println("DEBUG: Adjusted volume to: ${control.value} dB")
                    }
                }

                clip.start()
                println("DEBUG: Clip started")
                
                // Give the audio system a moment to actually start
                delay(100)
                println("DEBUG: After delay - isActive: ${clip.isActive}, isRunning: ${clip.isRunning}")
                
                // Use a different approach for Windows - check if audio is actually playing
                var lastFramePosition = clip.framePosition
                var stuckCounter = 0
                val maxStuckTime = 20 // 1 second (20 * 50ms)

                while (clip.framePosition < clip.frameLength && isSpeaking.get() && _state.value == TTSState.SPEAKING) {
                    delay(50)
                    
                    val currentPosition = clip.framePosition
                    if (currentPosition == lastFramePosition) {
                        stuckCounter++
                        if (stuckCounter >= maxStuckTime) {
                            println("DEBUG: Audio appears stuck at position $currentPosition, attempting restart")
                            clip.stop()
                            clip.framePosition = currentPosition
                            clip.start()
                            stuckCounter = 0
                        }
                    } else {
                        stuckCounter = 0
                        if (currentPosition % 10000 == 0) {
                            println("DEBUG: Progress: ${currentPosition}/${clip.frameLength} frames (${(currentPosition.toFloat() / clip.frameLength * 100).toInt()}%)")
                        }
                    }
                    lastFramePosition = currentPosition
                }

                println("DEBUG: Playback loop exited. framePosition: ${clip.framePosition}/${clip.frameLength}, isSpeaking: ${isSpeaking.get()}, state: ${_state.value}")

                // Ensure clip stops properly
                if (clip.isRunning) {
                    clip.stop()
                }
                
                delay(100)
                println("DEBUG: playAudio completed")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in playAudio: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private suspend fun resumeAudio() = withContext(Dispatchers.IO) {
        try {
            audioClip?.let { clip ->
                if (pausedPosition > 0) {
                    clip.microsecondPosition = pausedPosition
                }

                clip.start()

                // Wait for clip to finish by checking frame position
                while (clip.framePosition < clip.frameLength && isSpeaking.get() && _state.value == TTSState.SPEAKING) {
                    delay(50)
                }

                // Wait a bit more to ensure audio finishes
                delay(100)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun stop(): TTSResult = withContext(Dispatchers.IO) {
        try {
            // Stop audio playback
            audioClip?.let { clip ->
                clip.stop()
                clip.close()
            }
            audioClip = null

            // Stop jobs
            speechJob?.cancel()
            audioPlayJob?.cancel()
            speechJob = null
            audioPlayJob = null
            _currentWordIndex.value = -1

            // Reset state
            isPaused = false
            pausedPosition = 0
            currentAudioData = null

            _state.value = TTSState.STOPPED
            isSpeaking.set(false)
            _playbackInfo.value = _playbackInfo.value?.copy(isPlaying = false)

            // Reset to idle after stopping
            _state.value = TTSState.IDLE
            TTSResult.Success
        } catch (e: Exception) {
            TTSResult.Error("Failed to stop: ${e.message}", e)
        }
    }

    override fun isSpeaking(): Boolean {
        return isSpeaking.get() || _state.value == TTSState.SPEAKING
    }

    override fun getAvailableVoices(): List<String> {
        return try {
            Voice.getAvailableVoices().map { voice ->
                "${voice.getName()} (${voice.locale})"
            }
        } catch (_: Exception) {
            // Return default MaryTTS voice as fallback
            listOf("cmu-slt-hsmm (en-US)")
        }
    }

    override suspend fun setConfig(config: TTSConfig): TTSResult {
        currentConfig = config
        return TTSResult.Success
    }

    override fun release() {
        try {
            // Stop audio playback
            audioClip?.let { clip ->
                clip.stop()
                clip.close()
            }
            audioClip = null

            // Stop jobs
            speechJob?.cancel()
            audioPlayJob?.cancel()

            _currentWordIndex.value = -1
            _state.value = TTSState.IDLE
            isSpeaking.set(false)
            isInitialized.set(false)

            // Release MaryTTS resources
            maryTTS = null
        } catch (e: Exception) {
            // Ignore release errors
        }
    }
}
