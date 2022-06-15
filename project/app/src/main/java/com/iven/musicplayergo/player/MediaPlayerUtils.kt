package com.iven.musicplayergo.player


import android.media.MediaPlayer

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
    fun safePrepare(mediaPlayer: MediaPlayer) {
        with (mediaPlayer) {
            try {
                prepare()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safeReset(mediaPlayer: MediaPlayer) {
        with (mediaPlayer) {
            try {
                reset()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safeCheckIsPlaying(mediaPlayer: MediaPlayer): Boolean {
        with (mediaPlayer) {
            return try {
                isPlaying
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                false
            }
        }
    }
}
