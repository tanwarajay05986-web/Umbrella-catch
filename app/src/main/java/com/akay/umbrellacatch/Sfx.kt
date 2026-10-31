package com.akay.umbrellacatch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

// small sound effects made in code — no audio files needed
object Sfx {
    private const val RATE = 22050

    private fun tone(freq: Double, ms: Int, fadeOutMs: Int = 30): ShortArray {
        val n = RATE * ms / 1000
        val fade = RATE * fadeOutMs / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            var v = sin(2.0 * PI * freq * i / RATE)
            if (i > n - fade) v *= (n - i).toDouble() / fade
            out[i] = (v * 9000).toInt().toShort()
        }
        return out
    }

    private fun play(data: ShortArray) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fmt = AudioFormat.Builder()
                .setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack(
                attrs, fmt, data.size * 2, AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(data, 0, data.size)
            track.setNotificationMarkerPosition(data.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) {
                    try {
                        t.release()
                    } catch (e: Throwable) {
                    }
                }

                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.play()
        } catch (e: Throwable) {
        }
    }

    fun catch() {
        if (!Store.sound()) return
        Thread { play(tone(880.0, 110)) }.start()
    }
}
