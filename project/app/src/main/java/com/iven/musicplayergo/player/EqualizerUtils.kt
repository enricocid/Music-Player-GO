package com.iven.musicplayergo.player

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.iven.musicplayergo.R
import com.iven.musicplayergo.utils.Versioning


object EqualizerUtils {

    @JvmStatic
    @Suppress("DEPRECATION")
    fun hasEqualizer(context: Context): Boolean {
        val pm = context.packageManager
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        val ri = if (Versioning.isTiramisu()) {
            pm.resolveActivity(intent, ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return ri != null
    }

    @JvmStatic
    fun openAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            LocalBroadcastManager.getInstance(context).sendBroadcast(this)
        }
    }

    @JvmStatic
    fun closeAudioEffectSession(context: Context, sessionId: Int) {
        Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            LocalBroadcastManager.getInstance(context).sendBroadcast(this)
        }
    }

    @JvmStatic
    fun openEqualizer(activity: Activity, mediaPlayer: MediaPlayer) {
        when (mediaPlayer.audioSessionId) {
            AudioEffect.ERROR_BAD_VALUE -> Toast.makeText(
                activity,
                activity.getString(R.string.error_bad_id),
                Toast.LENGTH_SHORT
            ).show()
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
