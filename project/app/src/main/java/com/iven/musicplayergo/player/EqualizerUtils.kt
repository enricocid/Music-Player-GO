package com.iven.musicplayergo.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.Utils

object EqualizerUtils {

    private fun hasEqualizer(context: Context): Boolean {
        val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        val pm = context.packageManager
        val ri = pm.resolveActivity(effects, 0)
        return ri != null
    }

    internal fun openAudioEffectSession(context: Context, sessionId: Int) {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)
    }

    internal fun closeAudioEffectSession(context: Context, sessionId: Int) {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    internal fun openEqualizer(activity: Activity, mediaPlayer: MediaPlayer) {
        if (hasEqualizer(activity))
            when (mediaPlayer.audioSessionId) {
                AudioEffect.ERROR_BAD_VALUE -> notifyNoSessionId(activity)
                else -> {
                    try {
                        val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        effects.putExtra(
                            AudioEffect.EXTRA_AUDIO_SESSION,
                            mediaPlayer.audioSessionId
                        )
                        effects.putExtra(
                            AudioEffect.EXTRA_CONTENT_TYPE,
                            AudioEffect.CONTENT_TYPE_MUSIC
                        )
                        activity.startActivityForResult(effects, 0)
                    } catch (notFound: ActivityNotFoundException) {
                        notFound.printStackTrace()
                    }
                }
            } else
            Utils.makeToast(
                activity,
                activity.getString(R.string.no_eq)
            )
    }

    fun notifyNoSessionId(context: Context) {
        Utils.makeToast(context, context.getString(R.string.bad_id))
    }
}
