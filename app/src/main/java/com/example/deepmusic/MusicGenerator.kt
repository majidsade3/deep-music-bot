package com.example.deepmusic

import kotlin.math.*
import kotlin.random.Random

class MusicGenerator(private val sampleRate: Int = 44100) {

    // پارامترهای قابل تنظیم از UI
    var bassIntensity: Float = 1.0f
    var kickIntensity: Float = 1.0f
    var bpm: Int = 126

    private val darkScales = listOf(
        intArrayOf(0, 2, 3, 5, 7, 8, 11),
        intArrayOf(0, 1, 3, 5, 7, 8, 10),
        intArrayOf(0, 2, 3, 5, 7, 8, 10)
    )

    fun generate(durationSeconds: Int = 16, baseNote: Int = 45): ShortArray {
        val total = durationSeconds * sampleRate
        val buffer = FloatArray(total)
        val scale = darkScales.random()
        val beatSamples = (sampleRate * 60.0 / bpm).toInt()

        generateSubBass(buffer, baseNote, beatSamples)
        generateDistortedBass(buffer, scale, baseNote, beatSamples)
        generateKick(buffer, beatSamples)
        generateHats(buffer, beatSamples)
        generateDarkPad(buffer, scale, baseNote)
        generateColdMelody(buffer, scale, baseNote, beatSamples)
        generateRumble(buffer)

        applyReverb(buffer)
        applyDelay(buffer, beatSamples / 3)
        normalize(buffer)
        return toShort(buffer)
    }

    private fun generateSubBass(buffer: FloatArray, base: Int, beat: Int) {
        if (bassIntensity <= 0.01f) return
        val pattern = intArrayOf(0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 3, 0, 0, 0, 7, 0)
        val noteLen = beat / 4
        for (i in buffer.indices) {
            val step = (i / noteLen) % pattern.size
            val offset = pattern[step]
            if (offset < 0) continue
            val freq = midiToFreq(base - 12 + offset)
            val t = (i % noteLen).toDouble() / sampleRate
            val env = exp(-t * 8.0)
            buffer[i] += sin(2 * PI * freq * t).toFloat() * 0.55f * bassIntensity * env.toFloat()
        }
    }

    private fun generateDistortedBass(buffer: FloatArray, scale: IntArray, base: Int, beat: Int) {
        if (bassIntensity <= 0.01f) return
        val noteLen = beat / 2
        for (i in buffer.indices) {
            val step = (i / noteLen)
            if (step % 4 == 3) continue
            val noteOffset = scale[step % scale.size]
            val freq = midiToFreq(base + noteOffset)
            val t = (i % noteLen).toDouble() / sampleRate
            val env = exp(-t * 6.0)

            val phase = (t * freq) % 1.0
            val saw = (phase * 2 - 1)
            val distorted = tanh(saw * 3.5).toFloat()

            buffer[i] += distorted * 0.32f * bassIntensity * env.toFloat()
        }
    }

    private fun generateKick(buffer: FloatArray, beat: Int) {
        if (kickIntensity <= 0.01f) return
        val kickSteps = booleanArrayOf(
            true, false, false, false,
            true, false, false, false,
            true, false, false, true,
            false, false, true, false
        )
        val stepLen = beat / 4
        val kickLen = (sampleRate * 0.35).toInt()

        for (s in kickSteps.indices) {
            if (!kickSteps[s]) continue
            val start = s * stepLen
            for (i in 0 until kickLen) {
                val idx = start + i
                if (idx >= buffer.size) break
                val t = i.toDouble() / sampleRate
                val freq = 55.0 * exp(-t * 28) + 38.0
                val env = exp(-t * 7.0)
                buffer[idx] += (sin(2 * PI * freq * t) * 0.85 * kickIntensity * env).toFloat()
            }
        }
    }

    private fun generateHats(buffer: FloatArray, beat: Int) {
        val stepLen = beat / 4
        val hatLen = (sampleRate * 0.06).toInt()
        for (s in 0 until 16) {
            if (s % 2 == 1 && Random.nextFloat() > 0.65f) continue
            val start = s * stepLen
            for (i in 0 until hatLen) {
                val idx = start + i
                if (idx >= buffer.size) break
                val env = exp(-i.toDouble() / hatLen * 8)
                val noise = (Random.nextFloat() * 2 - 1)
                if (i > 0) {
                    val prev = (Random.nextFloat() * 2 - 1)
                    buffer[idx] += ((noise - prev) * 0.08 * env).toFloat()
                }
            }
        }
    }

    private fun generateDarkPad(buffer: FloatArray, scale: IntArray, base: Int) {
        val chord = intArrayOf(base + 12, base + 12 + scale[2], base + 12 + scale[4], base + 24)
        for (note in chord) {
            val freq = midiToFreq(note)
            for (i in buffer.indices) {
                val t = i.toDouble() / sampleRate
                val detune = 1.0 + 0.003 * sin(2 * PI * 0.15 * t)
                val lfo = 1.0 + 0.01 * sin(2 * PI * 0.08 * t)
                val wave = sin(2 * PI * freq * detune * lfo * t) +
                        sin(2 * PI * freq * 0.997 * t) * 0.5
                val fade = (sin(PI * i.toDouble() / buffer.size)).coerceAtLeast(0.0)
                buffer[i] += (wave * 0.06 * fade).toFloat()
            }
        }
    }

    private fun generateColdMelody(buffer: FloatArray, scale: IntArray, base: Int, beat: Int) {
        val noteLen = beat / 2
        var pos = 0
        while (pos < buffer.size) {
            if (Random.nextFloat() < 0.35f) {
                pos += noteLen
                continue
            }
            val octave = if (Random.nextBoolean()) 24 else 36
            val noteOffset = scale[Random.nextInt(scale.size)]
            val freq = midiToFreq(base + octave + noteOffset)
            val len = noteLen * Random.nextInt(1, 3)
            val end = minOf(pos + len, buffer.size)
            for (i in pos until end) {
                val t = (i - pos).toDouble() / sampleRate
                val noteDur = (end - pos).toDouble() / sampleRate
                val env = when {
                    t < 0.01 -> t / 0.01
                    t > noteDur - 0.3 -> (noteDur - t) / 0.3
                    else -> 1.0
                }.coerceIn(0.0, 1.0)
                val wave = sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * freq * 2 * t)
                buffer[i] += (wave * 0.09 * env).toFloat()
            }
            pos = end + (sampleRate * 0.05).toInt()
        }
    }

    private fun generateRumble(buffer: FloatArray) {
        var lastNoise = 0f
        for (i in buffer.indices) {
            val t = i.toDouble() / sampleRate
            val noise = (Random.nextFloat() * 2 - 1)
            lastNoise = lastNoise * 0.995f + noise * 0.005f
            val mod = (0.5 + 0.5 * sin(2 * PI * 0.07 * t)).toFloat()
            buffer[i] += lastNoise * 0.12f * mod
        }
    }

    private fun applyReverb(buffer: FloatArray) {
        val delays = intArrayOf(
            (sampleRate * 0.0297).toInt(),
            (sampleRate * 0.0371).toInt(),
            (sampleRate * 0.0411).toInt(),
            (sampleRate * 0.0437).toInt()
        )
        val feedback = 0.75f
        for (d in delays) {
            for (i in d until buffer.size) {
                buffer[i] += buffer[i - d] * feedback * 0.25f
            }
        }
    }

    private fun applyDelay(buffer: FloatArray, delaySamples: Int) {
        if (delaySamples <= 0) return
        var tap1 = 0f
        var tap2 = 0f
        for (i in buffer.indices) {
            val input = buffer[i]
            val out = tap1 * 0.35f + tap2 * 0.2f
            buffer[i] = input + out
            if (i >= delaySamples) {
                tap2 = tap1
                tap1 = input + buffer[i - delaySamples] * 0.4f
            }
        }
    }

    private fun normalize(buffer: FloatArray) {
        var max = 0f
        for (s in buffer) if (abs(s) > max) max = abs(s)
        if (max < 0.001f) return
        val gain = 0.92f / max
        for (i in buffer.indices) {
            buffer[i] = tanh(buffer[i] * gain * 1.2).toFloat()
        }
    }

    private fun toShort(buffer: FloatArray): ShortArray {
        return ShortArray(buffer.size) { i ->
            (buffer[i] * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
}
