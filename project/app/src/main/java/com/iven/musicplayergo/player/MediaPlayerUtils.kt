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
}
