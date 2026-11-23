package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.BuiltInSound
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class FocusAudioSynthesizer {

    @Volatile
    private var isRunning = false

    @Volatile
    private var volume: Float = 0.65f

    @Volatile
    private var loopEnabled: Boolean = true

    private var playbackThread: Thread? = null

    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
    }

    fun setLoopEnabled(loop: Boolean) {
        loopEnabled = loop
    }

    fun start(
        sound: BuiltInSound,
        initialVolume: Float,
        loop: Boolean,
        onFinished: () -> Unit
    ) {
        stop()
        volume = initialVolume.coerceIn(0f, 1f)
        loopEnabled = loop
        isRunning = true

        playbackThread = Thread({
            val sampleRate = 44_100
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
                .coerceAtLeast(sampleRate / 5 * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(encoding)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val samplesPerChunk = 2048
            val pcmBuffer = ShortArray(samplesPerChunk * 2)

            // Filter state for rain & white noise
            var brownL = 0.0
            var brownR = 0.0
            var lpWhiteL = 0.0
            var lpWhiteR = 0.0
            var dropEnv = 0.0

            // Binaural phase accumulators (wrapped at 2*PI to stay accurate during long sessions)
            var phaseLeft = 0.0
            var phaseRight = 0.0
            val (freqLeft, freqRight) = when (sound) {
                BuiltInSound.ALPHA -> 200.0 to 210.0   // 10 Hz Alpha
                BuiltInSound.BETA -> 220.0 to 238.0    // 18 Hz Beta
                BuiltInSound.GAMMA -> 240.0 to 280.0   // 40 Hz Gamma
                else -> 0.0 to 0.0
            }
            val incLeft = 2.0 * PI * freqLeft / sampleRate
            val incRight = 2.0 * PI * freqRight / sampleRate
            val twoPi = 2.0 * PI

            // If loop is false, play a single 60-second gentle cycle then stop
            val maxSamplesSinglePass = sampleRate * 60L
            var totalSamplesPlayed = 0L

            try {
                track.play()
                while (isRunning) {
                    val vol = volume.toDouble()
                    for (i in 0 until samplesPerChunk) {
                        val (leftSample, rightSample) = when (sound) {
                            BuiltInSound.RAIN -> {
                                val wL = Random.nextDouble(-1.0, 1.0)
                                val wR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.035 * wL) / 1.035
                                brownR = (brownR + 0.035 * wR) / 1.035
                                if (Random.nextInt(1800) == 0) {
                                    dropEnv = Random.nextDouble(0.18, 0.42)
                                }
                                dropEnv *= 0.996
                                val baseL = (brownL * 2.8 + wL * 0.14 + dropEnv * wL * 0.45)
                                val baseR = (brownR * 2.8 + wR * 0.14 + dropEnv * wR * 0.45)
                                baseL to baseR
                            }
                            BuiltInSound.WHITE_NOISE -> {
                                val wL = Random.nextDouble(-1.0, 1.0)
                                val wR = Random.nextDouble(-1.0, 1.0)
                                lpWhiteL = lpWhiteL * 0.45 + wL * 0.55
                                lpWhiteR = lpWhiteR * 0.45 + wR * 0.55
                                (lpWhiteL * 0.35) to (lpWhiteR * 0.35)
                            }
                            BuiltInSound.ALPHA, BuiltInSound.BETA, BuiltInSound.GAMMA -> {
                                val sL = sin(phaseLeft) * 0.32
                                val sR = sin(phaseRight) * 0.32
                                phaseLeft += incLeft
                                phaseRight += incRight
                                if (phaseLeft >= twoPi) phaseLeft -= twoPi
                                if (phaseRight >= twoPi) phaseRight -= twoPi
                                sL to sR
                            }
                        }

                        val scaledL = (leftSample * vol * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                        val scaledR = (rightSample * vol * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()

                        val idx = i * 2
                        pcmBuffer[idx] = scaledL
                        pcmBuffer[idx + 1] = scaledR
                    }

                    if (!isRunning) break
                    track.write(pcmBuffer, 0, pcmBuffer.size)

                    totalSamplesPlayed += samplesPerChunk
                    if (!loopEnabled && totalSamplesPlayed >= maxSamplesSinglePass) {
                        break
                    }
                }
            } catch (_: Throwable) {
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
                val finishedNaturally = isRunning
                isRunning = false
                if (finishedNaturally) {
                    onFinished()
                }
            }
        }, "FocusAudioSynthThread").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning = false
        playbackThread?.let { t ->
            runCatching { t.join(350L) }
        }
        playbackThread = null
    }
}
