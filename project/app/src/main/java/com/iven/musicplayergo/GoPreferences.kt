package com.iven.musicplayergo

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.NotificationAction
import com.iven.musicplayergo.models.SavedEqualizerSettings
import com.iven.musicplayergo.models.Sorting
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type


class GoPreferences(context: Context) {

    private val prefsSavedEqualizerSettings = "eq_settings_pref"
    private val prefsLatestVolume = "latest_volume_pref"
    private val prefsLatestPlaybackVel = "latest_playback_vel_pref"
    private val prefsLatestPlayedSong = "to_restore_song_pref"
    private val prefsFavorites = "favorite_songs_pref"
    private val prefsQueue = "queue_songs_pref"
    private val prefsIsQueue = "is_queue_pref"

    private val prefsTheme = "theme_pref"
    private val prefsThemeDef = "theme_pref_auto"
    private val prefsThemeBlack = "theme_pref_black"
    private val prefsAccent = "color_primary_pref"

    private val prefsActiveTabsDef = "active_tabs_def_pref"
    private val prefsActiveTabs = "active_tabs_pref"

    private val prefsCover = "covers_pref"

    private val prefsOnListEnded = "on_list_ended_pref"

    private val prefsSongsVisual = "song_visual_pref"

    private val prefsArtistsSorting = "sorting_artists_pref"
    private val prefsFoldersSorting = "sorting_folder_details_pref"
    private val prefsAlbumsSorting = "sorting_album_details_pref"
    private val prefsAllMusicSorting = "sorting_all_music_tab_pref"

    private val prefsFastSeek = "fast_seeking_pref"
    private val prefsNotificationActions = "notification_actions_pref"
    private val prefsEq = "eq_pref"
    private val prefsPreciseVolume = "precise_volume_pref"
    private val prefsFocus = "focus_pref"
    private val prefsHeadsetPlug = "headsets_pref"

    private val prefsAnim = "anim_pref"
    private val prefsFilter = "strings_filter_pref"
    private val prefsPlaybackVel = "playback_vel_pref"
    private val prefsIsContinueOnEnd = "continue_on_end_pref"
    private val prefsHasCompletedPlayback = "has_completed_playback_pref"
    private val prefsIsAskConfirmation = "ask_confirmation_pref"
    private val prefsLockRotation = "rotation_pref"

    private val prefsDetailsSorting = "details_sorting_pref"
    private val prefsLocale = "locale_pref"

    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val mMoshi = Moshi.Builder().build()

    // active fragments type
    private val typeActiveTabs = Types.newParameterizedType(List::class.java, String::class.java)

    // favorites is a list of Music
    private val typeFavorites = Types.newParameterizedType(List::class.java, Music::class.java)

    // sortings is a list of Sorting
    private val typeSorting = Types.newParameterizedType(List::class.java, Sorting::class.java)

    var latestVolume: Int
        get() = mPrefs.getInt(prefsLatestVolume, 100)
        set(value) = mPrefs.edit { putInt(prefsLatestVolume, value) }

    var latestPlaybackSpeed: Float
        get() = mPrefs.getFloat(prefsLatestPlaybackVel, 1.0F)
        set(value) = mPrefs.edit { putFloat(prefsLatestPlaybackVel, value) }

    var latestPlayedSong: Music?
        get() = getObjectForClass(prefsLatestPlayedSong, Music::class.java)
        set(value) = putObjectForClass(prefsLatestPlayedSong, value, Music::class.java)

    var savedEqualizerSettings: SavedEqualizerSettings?
        get() = getObjectForClass(
            prefsSavedEqualizerSettings,
            SavedEqualizerSettings::class.java
        )
        set(value) = putObjectForClass(
            prefsSavedEqualizerSettings,
            value,
            SavedEqualizerSettings::class.java
        )

    var favorites: List<Music>?
        get() = getObjectForType(prefsFavorites, typeFavorites)
        set(value) = putObjectForType(prefsFavorites, value, typeFavorites)

    var queue: List<Music>?
        get() = getObjectForType(prefsQueue, typeFavorites)
        set(value) = putObjectForType(prefsQueue, value, typeFavorites)

    var isQueue: Music?
        get() = getObjectForClass(prefsIsQueue, Music::class.java)
        set(value) = putObjectForClass(prefsIsQueue, value, Music::class.java)

    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit { putString(prefsTheme, value) }

    var isBlackTheme: Boolean
        get() = mPrefs.getBoolean(prefsThemeBlack, false)
        set(value) = mPrefs.edit { putBoolean(prefsThemeBlack, value) }

    var accent
        get() = mPrefs.getInt(prefsAccent, 3)
        set(value) = mPrefs.edit { putInt(prefsAccent, value) }

    var activeTabsDef: List<String>
        get() = getObjectForType(prefsActiveTabsDef, typeActiveTabs)
            ?: GoConstants.DEFAULT_ACTIVE_FRAGMENTS
        set(value) = putObjectForType(prefsActiveTabsDef, value, typeActiveTabs)

    var activeTabs: List<String>
        get() = getObjectForType(prefsActiveTabs, typeActiveTabs)
            ?: GoConstants.DEFAULT_ACTIVE_FRAGMENTS
        set(value) = putObjectForType(prefsActiveTabs, value, typeActiveTabs)

    var onListEnded
        get() = mPrefs.getString(prefsOnListEnded, GoConstants.CONTINUE)
        set(value) = mPrefs.edit { putString(prefsOnListEnded, value) }

    var isCovers: Boolean
        get() = mPrefs.getBoolean(prefsCover, false)
        set(value) = mPrefs.edit { putBoolean(prefsCover, value) }

    var songsVisualization
        get() = mPrefs.getString(prefsSongsVisual, GoConstants.FN)
        set(value) = mPrefs.edit { putString(prefsSongsVisual, value.toString()) }

    var artistsSorting
        get() = mPrefs.getInt(prefsArtistsSorting, GoConstants.ASCENDING_SORTING)
        set(value) = mPrefs.edit { putInt(prefsArtistsSorting, value) }

    var foldersSorting
        get() = mPrefs.getInt(prefsFoldersSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt(prefsFoldersSorting, value) }

    var albumsSorting
        get() = mPrefs.getInt(prefsAlbumsSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt(prefsAlbumsSorting, value) }

    var allMusicSorting
        get() = mPrefs.getInt(prefsAllMusicSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt(prefsAllMusicSorting, value) }

    var notificationActions: NotificationAction
        get() = getObjectForType(prefsNotificationActions, NotificationAction::class.java)
            ?: NotificationAction(GoConstants.REPEAT_ACTION, GoConstants.CLOSE_ACTION)
        set(value) = putObjectForType(prefsNotificationActions, value, NotificationAction::class.java)

    var filters: Set<String>?
        get() = mPrefs.getStringSet(prefsFilter, setOf())
        set(value) = mPrefs.edit { putStringSet(prefsFilter, value) }

    var fastSeekingStep
        get() = mPrefs.getInt(prefsFastSeek, 5)
        set(value) = mPrefs.edit { putInt(prefsFastSeek, value) }

    var isEqForced
        get() = mPrefs.getBoolean(prefsEq, false)
        set(value) = mPrefs.edit { putBoolean(prefsEq, value) }

    var isPreciseVolumeEnabled
        get() = mPrefs.getBoolean(prefsPreciseVolume, true)
        set(value) = mPrefs.edit { putBoolean(prefsPreciseVolume, value) }

    var isFocusEnabled
        get() = mPrefs.getBoolean(prefsFocus, true)
        set(value) = mPrefs.edit { putBoolean(prefsFocus, value) }

    var isHeadsetPlugEnabled
        get() = mPrefs.getBoolean(prefsHeadsetPlug, true)
        set(value) = mPrefs.edit { putBoolean(prefsHeadsetPlug, value) }

    var playbackSpeedMode
        get() = mPrefs.getString(prefsPlaybackVel, GoConstants.PLAYBACK_SPEED_ONE_ONLY)
        set(value) = mPrefs.edit { putString(prefsPlaybackVel, value) }

    var isAnimations
        get() = mPrefs.getBoolean(prefsAnim, true)
        set(value) = mPrefs.edit { putBoolean(prefsAnim, value) }

    var continueOnEnd
        get() = mPrefs.getBoolean(prefsIsContinueOnEnd, true)
        set(value) = mPrefs.edit { putBoolean(prefsIsContinueOnEnd, value) }

    var hasCompletedPlayback
        get() = mPrefs.getBoolean(prefsHasCompletedPlayback, false)
        set(value) = mPrefs.edit { putBoolean(prefsHasCompletedPlayback, value) }

    var locale
        get() = mPrefs.getString(prefsLocale, null)
        set(value) = mPrefs.edit { putString(prefsLocale, value) }

    var isAskForRemoval: Boolean
        get() = mPrefs.getBoolean(prefsIsAskConfirmation, true)
        set(value) = mPrefs.edit { putBoolean(prefsIsAskConfirmation, value) }

    var lockRotation: Boolean
        get() = mPrefs.getBoolean(prefsLockRotation, false)
        set(value) = mPrefs.edit { putBoolean(prefsLockRotation, value) }

    var sortings: List<Sorting>?
        get() = getObjectForType(prefsDetailsSorting, typeSorting)
        set(value) = putObjectForType(prefsDetailsSorting, value, typeSorting)

    // Retrieve object from the Preferences using Moshi
    private fun <T : Any> putObjectForType(key: String, value: T?, type: Type) {
        val json = mMoshi.adapter<T>(type).toJson(value)
        mPrefs.edit { putString(key, json) }
    }

    private fun <T : Any> getObjectForType(key: String, type: Type): T? {
        val json = mPrefs.getString(key, null)
        return if (json == null) {
            null
        } else {
            try {
                mMoshi.adapter<T>(type).fromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Saves object into the Preferences using Moshi
    private fun <T : Any> putObjectForClass(key: String, value: T?, clazz: Class<T>) {
        val json = mMoshi.adapter(clazz).toJson(value)
        mPrefs.edit { putString(key, json) }
    }

    private fun <T : Any> getObjectForClass(key: String, clazz: Class<T>): T? {
        val json = mPrefs.getString(key, null)
        return if (json == null) {
            null
        } else {
            try {
                mMoshi.adapter(clazz).fromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        // Singleton prevents multiple instances of GoPreferences opening at the
        // same time.
        @Volatile
        private var INSTANCE: GoPreferences? = null

        fun initPrefs(context: Context): GoPreferences {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the preferences
            return INSTANCE ?: synchronized(this) {
                val instance = GoPreferences(context)
                INSTANCE = instance
                // return instance
                instance
            }
        }

        fun getPrefsInstance(): GoPreferences {
            return INSTANCE ?: error("Preferences not initialized!")
        }
    }
}
