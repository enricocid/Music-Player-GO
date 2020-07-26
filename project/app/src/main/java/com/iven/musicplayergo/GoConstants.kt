package com.iven.musicplayergo

import android.support.v4.media.session.PlaybackStateCompat

object GoConstants {

    const val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2588
    const val RESTORE_SETTINGS_FRAGMENT = "RESTORE_SETTINGS_FRAGMENT"

    const val IS_APPLY_THEME = "IS_APPLY_THEME"

    // error tags
    const val TAG_NO_PERMISSION = "NO_PERMISSION"
    const val TAG_NO_MUSIC = "NO_MUSIC"
    const val TAG_NO_MUSIC_INTENT = "NO_MUSIC_INTENT"
    const val TAG_SD_NOT_READY = "SD_NOT_READY"

    // tags for fragments bundles
    const val TAG_ARTIST_FOLDER = "SELECTED_ARTIST_FOLDER"
    const val TAG_IS_FOLDER = "IS_FOLDER"
    const val TAG_SELECTED_ALBUM_POSITION = "SELECTED_ALBUM_POSITION"
    const val TAG_ERROR = "WE_HAVE_A_PROBLEM_HOUSTON"

    // Player playing statuses
    const val PLAYING = PlaybackStateCompat.STATE_PLAYING
    const val PAUSED = PlaybackStateCompat.STATE_PAUSED
    const val RESUMED = PlaybackStateCompat.STATE_NONE

    // Notification
    const val NOTIFICATION_ID = 101
    const val PREV_ACTION = "PREV_GO"
    const val PLAY_PAUSE_ACTION = "PLAY_PAUSE_GO"
    const val NEXT_ACTION = "NEXT_GO"
    const val REPEAT_ACTION = "REPEAT_GO"
    const val CLOSE_ACTION = "CLOSE_GO"
}
