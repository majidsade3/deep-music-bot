package com.example.deepmusic

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MusicPlayer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val generator = MusicGenerator()

    @Volatile private var isPlaying = false
    @Volatile private var saveNextTrack = false

    var onNewTrack: ((Int) -> Unit)? = null
    var onSaved: ((String?) -> Unit)? = null

    var bassIntensity: Float
        get() = generator.bassIntensity
        set(v) { generator.bassIntensity = v }

    var kickIntensity: Float
        get() = generator.kickIntensity
        set(v) { generator.kickIntensity = v }

    var bpm: Int
        get() = generator.bpm
        set(v) { generator.bpm = v }

    fun start() {
        if (isPlaying) return
        isPlaying = true
        job = scope.launch {
            var trackCount = 0
            while (isActive && isPlaying) {
                trackCount++
                withContext(Dispatchers.Main) { onNewTrack?.invoke(trackCount) }

                val baseNote = listOf(41, 43, 45, 46, 48).random()
                val duration = Random.nextInt(14, 22)
                val samples = generator.generate(duration, baseNote)

                if (saveNextTrack) {
                    saveNextTrack = false
                    val name = "DeepTrack_${System.currentTimeMillis()}"
                    val path = WavSaver.saveToMusic(context, samples, name)
                    withContext(Dispatchers.Main) { onSaved?.invoke(path) }
                }

                playSamples(samples)
            }
        }
    }

    fun requestSaveNextTrack() {
        saveNextTrack = true
    }

    private suspend fun playSamples(samples: ShortArray) {
        val bufferSize = AudioTrack.getMinBufferSize(
            44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()

        var offset = 0
        val chunkSize = 4096
        while (offset < samples.size && isPlaying) {
            val end = minOf(offset + chunkSize, samples.size)
            track.write(samples, offset, end - offset)
            offset = end
            if (!currentCoroutineContext().isActive) break
        }

        delay(500)
        track.stop()
        track.release()
    }

    fun stop() {
        isPlaying = false
        job?.cancel()
    }
}
