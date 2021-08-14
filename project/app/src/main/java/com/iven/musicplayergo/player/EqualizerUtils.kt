package com.iven.musicplayergo.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.widget.Toast
import com.iven.musicplayergo.R


object EqualizerUtils {

    @JvmStatic
    fun hasEqualizer(context: Context): Boolean {
        val pm = context.packageManager
        val ri =
            pm.resolveActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL), 0)
        return ri != null
    }

    @JvmStatic
    fun openAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            context.sendBroadcast(this)
        }
    }

    @JvmStatic
    fun closeAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            context.sendBroadcast(this)
        }
    }

    @JvmStatic
    fun openEqualizer(activity: Activity, mediaPlayer: MediaPlayer) {
        when (mediaPlayer.audioSessionId) {
            AudioEffect.ERROR_BAD_VALUE -> Toast.makeText(activity, R.string.error_bad_id, Toast.LENGTH_LONG)
                .show()
            else -> {
                try {
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(
                            AudioEffect.EXTRA_AUDIO_SESSION,
                            mediaPlayer.audioSessionId
                        )
                        putExtra(
                            AudioEffect.EXTRA_CONTENT_TYPE,
                            AudioEffect.CONTENT_TYPE_MUSIC
                        )
                        activity.startActivityForResult(this, 0)
                    }
                } catch (notFound: ActivityNotFoundException) {
                    notFound.printStackTrace()
                }
            }
        }
    }
}
