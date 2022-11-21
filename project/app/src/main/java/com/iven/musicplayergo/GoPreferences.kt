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

    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val mMoshi = Moshi.Builder().build()

    // active fragments type
    private val typeActiveTabs = Types.newParameterizedType(List::class.java, String::class.java)

    // favorites is a list of Music
    private val typeFavorites = Types.newParameterizedType(List::class.java, Music::class.java)

    // sortings is a list of Sorting
    private val typeSorting = Types.newParameterizedType(List::class.java, Sorting::class.java)

    var latestVolume: Int
        get() = mPrefs.getInt("latest_volume_pref", 100)
        set(value) = mPrefs.edit { putInt("latest_volume_pref", value) }

    var latestPlaybackSpeed: Float
        get() = mPrefs.getFloat("latest_playback_vel_pref", 1.0F)
        set(value) = mPrefs.edit { putFloat("latest_playback_vel_pref", value) }

    var latestPlayedSong: Music?
        get() = getObjectForClass("to_restore_song_pref", Music::class.java)
        set(value) = putObjectForClass("to_restore_song_pref", value, Music::class.java)

    var savedEqualizerSettings: SavedEqualizerSettings?
        get() = getObjectForClass("eq_settings_pref", SavedEqualizerSettings::class.java)
        set(value) = putObjectForClass(
            "eq_settings_pref",
            value,
            SavedEqualizerSettings::class.java
        )

    var favorites: List<Music>?
        get() = getObjectForType("favorite_songs_pref", typeFavorites)
        set(value) = putObjectForType("favorite_songs_pref", value, typeFavorites)

    var queue: List<Music>?
        get() = getObjectForType("queue_songs_pref", typeFavorites)
        set(value) = putObjectForType("queue_songs_pref", value, typeFavorites)

    var isQueue: Music?
        get() = getObjectForClass("is_queue_pref", Music::class.java)
        set(value) = putObjectForClass("is_queue_pref", value, Music::class.java)

    var theme
        get() = mPrefs.getString("theme_pref", "theme_pref_auto")
        set(value) = mPrefs.edit { putString("theme_pref", value) }

    var isBlackTheme: Boolean
        get() = mPrefs.getBoolean("theme_pref_black", false)
        set(value) = mPrefs.edit { putBoolean("theme_pref_black", value) }

    var accent
        get() = mPrefs.getInt("color_primary_pref", 3)
        set(value) = mPrefs.edit { putInt("color_primary_pref", value) }

    var activeTabsDef: List<String>
        get() = getObjectForType("active_tabs_def_pref", typeActiveTabs)
            ?: GoConstants.DEFAULT_ACTIVE_FRAGMENTS
        set(value) = putObjectForType("active_tabs_def_pref", value, typeActiveTabs)

    var activeTabs: List<String>
        get() = getObjectForType("active_tabs_pref", typeActiveTabs)
            ?: GoConstants.DEFAULT_ACTIVE_FRAGMENTS
        set(value) = putObjectForType("active_tabs_pref", value, typeActiveTabs)

    var onListEnded
        get() = mPrefs.getString("on_list_ended_pref", GoConstants.CONTINUE)
        set(value) = mPrefs.edit { putString("on_list_ended_pref", value) }

    var isCovers: Boolean
        get() = mPrefs.getBoolean("covers_pref", false)
        set(value) = mPrefs.edit { putBoolean("covers_pref", value) }

    var songsVisualization
        get() = mPrefs.getString("song_visual_pref", GoConstants.FN)
        set(value) = mPrefs.edit { putString("song_visual_pref", value.toString()) }

    var artistsSorting
        get() = mPrefs.getInt("sorting_artists_pref", GoConstants.ASCENDING_SORTING)
        set(value) = mPrefs.edit { putInt("sorting_artists_pref", value) }

    var foldersSorting
        get() = mPrefs.getInt("sorting_folder_details_pref", GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt("sorting_folder_details_pref", value) }

    var albumsSorting
        get() = mPrefs.getInt("sorting_album_details_pref", GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt("sorting_album_details_pref", value) }

    var allMusicSorting
        get() = mPrefs.getInt("sorting_all_music_tab_pref", GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit { putInt("sorting_all_music_tab_pref", value) }

    var notificationActions: NotificationAction
        get() = getObjectForType("notification_actions_pref", NotificationAction::class.java)
            ?: NotificationAction(GoConstants.REPEAT_ACTION, GoConstants.CLOSE_ACTION)
        set(value) = putObjectForType("notification_actions_pref", value, NotificationAction::class.java)

    var filters: Set<String>?
        get() = mPrefs.getStringSet("strings_filter_pref", setOf())
        set(value) = mPrefs.edit { putStringSet("strings_filter_pref", value) }

    var fastSeekingStep
        get() = mPrefs.getInt("fast_seeking_pref", 5)
        set(value) = mPrefs.edit { putInt("fast_seeking_pref", value) }

    var isEqForced
        get() = mPrefs.getBoolean("eq_pref", false)
        set(value) = mPrefs.edit { putBoolean("eq_pref", value) }

    var isPreciseVolumeEnabled
        get() = mPrefs.getBoolean("precise_volume_pref", true)
        set(value) = mPrefs.edit { putBoolean("precise_volume_pref", value) }

    var isFocusEnabled
        get() = mPrefs.getBoolean("focus_pref", true)
        set(value) = mPrefs.edit { putBoolean("focus_pref", value) }

    var isHeadsetPlugEnabled
        get() = mPrefs.getBoolean("headsets_pref", true)
        set(value) = mPrefs.edit { putBoolean("headsets_pref", value) }

    var playbackSpeedMode
        get() = mPrefs.getString("playback_vel_pref", GoConstants.PLAYBACK_SPEED_ONE_ONLY)
        set(value) = mPrefs.edit { putString("playback_vel_pref", value) }

    var isAnimations
        get() = mPrefs.getBoolean("anim_pref", true)
        set(value) = mPrefs.edit { putBoolean("anim_pref", value) }

    var continueOnEnd
        get() = mPrefs.getBoolean("continue_on_end_pref", true)
        set(value) = mPrefs.edit { putBoolean("continue_on_end_pref", value) }

    var hasCompletedPlayback
        get() = mPrefs.getBoolean("has_completed_playback_pref", false)
        set(value) = mPrefs.edit { putBoolean("has_completed_playback_pref", value) }

    var locale
        get() = mPrefs.getString("locale_pref", null)
        set(value) = mPrefs.edit { putString("locale_pref", value) }

    var isAskForRemoval: Boolean
        get() = mPrefs.getBoolean("ask_confirmation_pref", true)
        set(value) = mPrefs.edit { putBoolean("ask_confirmation_pref", value) }

    var lockRotation: Boolean
        get() = mPrefs.getBoolean("rotation_pref", false)
        set(value) = mPrefs.edit { putBoolean("rotation_pref", value) }

    var isSetDefSorting: Boolean
        get() = mPrefs.getBoolean("ask_sorting_pref", true)
        set(value) = mPrefs.edit { putBoolean("ask_sorting_pref", value) }

    var sortings: List<Sorting>?
        get() = getObjectForType(PREFS_DETAILS_SORTING, typeSorting)
        set(value) = putObjectForType(PREFS_DETAILS_SORTING, value, typeSorting)

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

        const val PREFS_DETAILS_SORTING = "details_sorting_pref"

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
