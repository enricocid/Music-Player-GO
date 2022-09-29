package com.iven.musicplayergo

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.NotificationAction
import com.iven.musicplayergo.models.SavedEqualizerSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type


class GoPreferences(context: Context) {

    private val prefsSavedEqualizerSettings = context.getString(R.string.saved_eq_settings)
    private val prefsLatestVolume = context.getString(R.string.latest_volume_pref)
    private val prefsLatestPlaybackVel = context.getString(R.string.latest_playback_vel_pref)
    private val prefsLatestPlayedSong = context.getString(R.string.latest_played_song_pref)
    private val prefsFavorites = context.getString(R.string.favorites_pref)
    private val prefsQueue = context.getString(R.string.queue_pref)
    private val prefsIsQueue = context.getString(R.string.is_queue_pref)

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDef = context.getString(R.string.theme_pref_auto)
    private val prefsThemeBlack = context.getString(R.string.theme_pref_black)
    private val prefsAccent = context.getString(R.string.accent_pref)

    private val prefsActiveTabsDef = context.getString(R.string.active_tabs_def_pref)
    private val prefsActiveTabs = context.getString(R.string.active_tabs_pref)

    private val prefsCover = context.getString(R.string.covers_pref)

    private val prefsOnListEnded = context.getString(R.string.on_list_ended_pref)

    private val prefsSongsVisual = context.getString(R.string.song_visual_pref)

    private val prefsArtistsSorting = context.getString(R.string.artists_sorting_pref)
    private val prefsFoldersSorting = context.getString(R.string.folders_sorting_pref)
    private val prefsAlbumsSorting = context.getString(R.string.albums_sorting_pref)
    private val prefsAllMusicSorting = context.getString(R.string.all_music_sorting_pref)

    private val prefsFastSeek = context.getString(R.string.fast_seeking_pref)
    private val prefsNotificationActions = context.getString(R.string.notif_actions_pref)
    private val prefsEq = context.getString(R.string.eq_pref)
    private val prefsPreciseVolume = context.getString(R.string.precise_volume_pref)
    private val prefsFocus = context.getString(R.string.focus_pref)
    private val prefsHeadsetPlug = context.getString(R.string.headset_pref)

    private val prefsAnim = context.getString(R.string.anim_pref)
    private val prefsFilter = context.getString(R.string.filter_pref)
    private val prefsPlaybackVel = context.getString(R.string.playback_vel_pref)
    private val prefsIsContinueOnEnd = context.getString(R.string.continue_on_end_pref)
    private val prefsHasCompletedPlayback = context.getString(R.string.has_completed_playback_pref)

    private val prefsLocale = context.getString(R.string.locale_pref)

    private val prefsIsAskConfirmation = context.getString(R.string.ask_confirmation_pref)

    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val mMoshi = Moshi.Builder().build()

    // active fragments type
    private val typeActiveTabs = Types.newParameterizedType(List::class.java, String::class.java)

    // favorites is a list of Music
    private val typeFavorites = Types.newParameterizedType(List::class.java, Music::class.java)

    var latestVolume: Int
        get() = mPrefs.getInt(prefsLatestVolume, 100)
        set(value) = mPrefs.edit { putInt(prefsLatestVolume, value) }

    var latestPlaybackSpeed: Float
        get() = mPrefs.getFloat(prefsLatestPlaybackVel, 1.0F)
        set(value) = mPrefs.edit { putFloat(prefsLatestPlaybackVel, value) }

    var latestPlayedSong: Music?
        get() = getObjectForClass(
            prefsLatestPlayedSong,
            Music::class.java
        )
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
        get() = getObjectForType(
            prefsFavorites,
            typeFavorites
        )
        set(value) = putObjectForType(prefsFavorites, value, typeFavorites)

    var queue: List<Music>?
        get() = getObjectForType(
            prefsQueue,
            typeFavorites
        )
        set(value) = putObjectForType(prefsQueue, value, typeFavorites)

    var isQueue: Music?
        get() = getObjectForClass(
            prefsIsQueue,
            Music::class.java
        )
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
            return INSTANCE ?: error("GoPreferences not initialized!")
        }
    }
}
