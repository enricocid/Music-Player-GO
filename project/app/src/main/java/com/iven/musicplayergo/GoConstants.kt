package com.iven.musicplayergo

import android.support.v4.media.session.PlaybackStateCompat


object GoConstants {

    const val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2588
    const val RESTORE_SETTINGS_FRAGMENT = "RESTORE_SETTINGS_FRAGMENT"
    const val LAUNCHED_BY_TILE = "LAUNCHED_BY_TILE"

    // active fragments
    const val ARTISTS_TAB = "ARTISTS_TAB"
    const val ALBUM_TAB = "ALBUM_TAB"
    const val SONGS_TAB = "SONGS_TAB"
    const val FOLDERS_TAB = "FOLDERS_TAB"
    const val SETTINGS_TAB = "SETTINGS_TAB"

    val DEFAULT_ACTIVE_FRAGMENTS =
        listOf(ARTISTS_TAB, ALBUM_TAB, SONGS_TAB, FOLDERS_TAB, SETTINGS_TAB)

    // launched by, used to determine which MusicContainerListFragment is instantiated by the ViewPager
    const val ARTIST_VIEW = "0"
    const val ALBUM_VIEW = "1"
    const val FOLDER_VIEW = "2"

    // playback speed options
    const val PLAYBACK_SPEED_ONE_ONLY = "0"

    // on list ended option
    const val CONTINUE = "0"

    // song visualization options
    const val FN = "1"

    // notification options
    const val NOTIF_REPEAT_CLOSE = "0"

    // sorting
    const val DEFAULT_SORTING = 0
    const val ASCENDING_SORTING = 1
    const val DESCENDING_SORTING = 2
    const val TRACK_SORTING = 3
    const val TRACK_SORTING_INVERTED = 4
    const val DATE_ADDED_SORTING = 5
    const val DATE_ADDED_SORTING_INV = 6
    const val ARTIST_SORTING = 7
    const val ARTIST_SORTING_INV = 8
    const val ALBUM_SORTING = 9
    const val ALBUM_SORTING_INV = 10

    // error tags
    const val TAG_NO_PERMISSION = "NO_PERMISSION"
    const val TAG_NO_MUSIC = "NO_MUSIC"
    const val TAG_NO_MUSIC_INTENT = "NO_MUSIC_INTENT"
    const val TAG_SD_NOT_READY = "SD_NOT_READY"

    // fragments tags
    const val DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT"
    const val ERROR_FRAGMENT_TAG = "ERROR_FRAGMENT"
    const val EQ_FRAGMENT_TAG = "EQ_FRAGMENT"

    // Player playing statuses
    const val PLAYING = PlaybackStateCompat.STATE_PLAYING
    const val PAUSED = PlaybackStateCompat.STATE_PAUSED
    const val RESUMED = PlaybackStateCompat.STATE_NONE

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "CHANNEL_ID_GO"
    const val NOTIFICATION_INTENT_REQUEST_CODE = 100
    const val NOTIFICATION_ID = 101
    const val FAST_FORWARD_ACTION = "FAST_FORWARD_GO"
    const val PREV_ACTION = "PREV_GO"
    const val PLAY_PAUSE_ACTION = "PLAY_PAUSE_GO"
    const val NEXT_ACTION = "NEXT_GO"
    const val REWIND_ACTION = "REWIND_GO"
    const val REPEAT_ACTION = "REPEAT_GO"
    const val CLOSE_ACTION = "CLOSE_GO"

    // Modal rv type
    const val ACCENT_TYPE = "MODAL_ACCENT"
    const val TABS_TYPE = "MODAL_TABS"
    const val FILTERS_TYPE = "MODAL_FILTERS"
    const val QUEUE_TYPE = "MODAL_QUEUE"
    const val FAV_TYPE = "MODAL_FAVORITES"
    const val SLEEPTIMER_TYPE = "MODAL_SLEEPTIMER"
    const val SLEEPTIMER_ELAPSED_TYPE = "MODAL_SLEEPTIMER_ELAPSED"
}
