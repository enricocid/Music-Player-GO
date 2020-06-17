package com.iven.musicplayergo.player

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.VersioningHelper

// The volume we set the media player to when we lose audio focus, but are
// allowed to reduce the volume instead of stopping playback.
private const val VOLUME_DUCK = 0.2f

// The volume we set the media player when we have audio focus.
private const val VOLUME_NORMAL = 1.0f

// We don't have audio focus, and can't duck (play at a low volume)
private const val AUDIO_NO_FOCUS_NO_DUCK = 0

// We have full audio focus
private const val AUDIO_FOCUSED = 1

//https://developer.android.com/guide/topics/media-apps/audio-focus
class AudioFocusHandler(
    private val audioManager: AudioManager
) {

    private val sFocusEnabled get() = goPreferences.isFocusEnabled
    private val mHandler = Handler()
    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var sPlayOnFocusGain = false
    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    private val mOnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            mMediaPlayerHolder.apply {
                if (mMediaPlayerHolder.isPlay && getMediaPlayerInstance()?.isPlaying!!) {
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS ->
                            // Permanent loss of audio focus
                            // Pause playback immediately
                            mMediaPlayerHolder.pauseMediaPlayer()

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            // Pause playback
                            mMediaPlayerHolder.pauseMediaPlayer()
                            sPlayOnFocusGain =
                                mMediaPlayerHolder.state == GoConstants.PLAYING || mMediaPlayerHolder.state == GoConstants.RESUMED
                        }

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            // Lower the volume, keep playing
                            getMediaPlayerInstance()?.setVolume(
                                VOLUME_DUCK,
                                VOLUME_DUCK
                            )
                            sPlayOnFocusGain = false
                        }


                        AudioManager.AUDIOFOCUS_GAIN -> {
                            // Your app has been granted audio focus again
                            // Raise volume to normal, restart playback if necessary
                            if (sPlayOnFocusGain) {
                                mMediaPlayerHolder.resumeMediaPlayer()
                            } else {
                                getMediaPlayerInstance()?.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
                            }
                        }
                    }
                }
            }

        }

    fun tryToGetAudioFocus() {
        if (sFocusEnabled) {
            mCurrentAudioFocusState = when (res) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> AUDIO_FOCUSED
                else -> AUDIO_NO_FOCUS_NO_DUCK
            }
        }
    }

    fun handleFocusPrefChange(enabled: Boolean) {
        if (enabled) {
            tryToGetAudioFocus()
        } else {
            giveUpAudioFocus()
        }
    }

    @TargetApi(26)
    val audioFocusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener(mOnAudioFocusChangeListener, mHandler)
            build()
        }

    @Suppress("DEPRECATION")
    val res = if (VersioningHelper.isOreoMR1()) {
        audioManager.requestAudioFocus(audioFocusRequest)
    } else {
        audioManager.requestAudioFocus(
            mOnAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    @Suppress("DEPRECATION")
    fun giveUpAudioFocus() {
        if (sFocusEnabled) {
            if (VersioningHelper.isOreo()) {
                audioManager.abandonAudioFocusRequest(
                    audioFocusRequest
                )
            } else {
                audioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
            }
        }
    }
}
