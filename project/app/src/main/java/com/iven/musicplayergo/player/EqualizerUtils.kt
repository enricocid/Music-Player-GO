package com.iven.musicplayergo.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toToast

object EqualizerUtils {

    private fun hasEqualizer(context: Context): Boolean {
        val pm = context.packageManager
        val ri =
                pm.resolveActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL), 0)
        return ri != null
    }

    internal fun openAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            context.sendBroadcast(this)
        }
    }

    internal fun closeAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            context.sendBroadcast(this)
        }
    }

    internal fun openEqualizer(activity: Activity, mediaPlayer: MediaPlayer) {
        if (hasEqualizer(activity)) {
            when (mediaPlayer.audioSessionId) {
                AudioEffect.ERROR_BAD_VALUE -> activity.getString(R.string.error_bad_id).toToast(
                        activity
                )
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
        } else {
            activity.getString(R.string.error_no_eq).toToast(activity)
        }
    }
}
