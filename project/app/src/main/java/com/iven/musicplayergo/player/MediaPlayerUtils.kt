package com.iven.musicplayergo.player


import android.annotation.TargetApi
import android.media.MediaPlayer
import android.os.Build


object MediaPlayerUtils {

    @JvmStatic
    fun safePause(mediaPlayer: MediaPlayer) {
        with(mediaPlayer) {
            try {
                if (isPlaying) {
                    pause()
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safePlay(mediaPlayer: MediaPlayer) {
        with (mediaPlayer) {
            try {
                start()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.M)
    fun safeSetPlaybackSpeed(mediaPlayer: MediaPlayer, speed: Float) {
        with(mediaPlayer) {
            try {
                playbackParams = playbackParams.setSpeed(speed)
            } catch (e: IllegalArgumentException) {
                playbackParams = playbackParams.setSpeed(1.0F)
                e.printStackTrace()
            }
        }
    }
}
