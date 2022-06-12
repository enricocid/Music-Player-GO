package com.iven.musicplayergo.player


import android.media.MediaPlayer
import java.lang.Exception

object MediaPlayerUtils {

    @JvmStatic
    fun safePause(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.run {
            try {
                if (isPlaying) {
                    pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safePlay(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.run {
            try {
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safePrepare(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.run {
            try {
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safeReset(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.run {
            try {
                reset()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun safeCheckIsPlaying(mediaPlayer: MediaPlayer?): Boolean {
        mediaPlayer?.run {
            return try {
                isPlaying
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }
}
