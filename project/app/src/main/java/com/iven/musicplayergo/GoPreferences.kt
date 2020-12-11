package com.iven.musicplayergo

import android.content.Context
import androidx.preference.PreferenceManager
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.models.SavedEqualizerSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type


class GoPreferences(context: Context) {

    private val prefsSavedEqualizerSettings = context.getString(R.string.saved_eq_settings)
    private val prefsLatestVolume = context.getString(R.string.latest_volume_pref)
    private val prefsLatestPlayedSong = context.getString(R.string.latest_played_song_pref)
    private val prefsLovedSongs = context.getString(R.string.loved_songs_pref)

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDef = context.getString(R.string.theme_pref_auto)
    private val prefsAccent = context.getString(R.string.accent_pref)

    private val prefsDefaultFragments = mutableListOf(GoConstants.ARTISTS_TAB, GoConstants.ALBUM_TAB, GoConstants.SONGS_TAB, GoConstants.FOLDERS_TAB, GoConstants.SETTINGS_TAB)
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
    private val prefsFastSeekActions = context.getString(R.string.fast_seeking_actions_pref)
    private val prefsPreciseVolume = context.getString(R.string.precise_volume_pref)
    private val prefsFocus = context.getString(R.string.focus_pref)
    private val prefsHeadsetPlug = context.getString(R.string.headset_pref)

    private val prefsFilter = context.getString(R.string.filter_pref)

    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val mMoshi = Moshi.Builder().build()

    // active fragments type
    private val typeActiveTabs = Types.newParameterizedType(MutableList::class.java, String::class.java)

    //loved songs is a list of Music
    private val typeLovedSongs = Types.newParameterizedType(MutableList::class.java, Music::class.java)

    var latestVolume: Int
        get() = mPrefs.getInt(prefsLatestVolume, 100)
        set(value) = mPrefs.edit().putInt(prefsLatestVolume, value).apply()

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
        set(value) = putObjectForClass(prefsSavedEqualizerSettings, value, SavedEqualizerSettings::class.java)

    var lovedSongs: MutableList<Music>?
        get() = getObjectForType(
                prefsLovedSongs,
                typeLovedSongs
        )
        set(value) = putObjectForType(prefsLovedSongs, value, typeLovedSongs)

    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit().putString(prefsTheme, value).apply()

    var accent
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
        set(value) = mPrefs.edit().putInt(prefsAccent, value).apply()

    var activeTabsDef: MutableList<String>
        get() = getObjectForType(prefsActiveTabsDef, typeActiveTabs)
                ?: prefsDefaultFragments
        set(value) = putObjectForType(prefsActiveTabsDef, value, typeActiveTabs)

    var activeTabs: MutableList<String>
        get() = getObjectForType(prefsActiveTabs, typeActiveTabs)
                ?: prefsDefaultFragments
        set(value) = putObjectForType(prefsActiveTabs, value, typeActiveTabs)

    var onListEnded
        get() = mPrefs.getString(prefsOnListEnded, GoConstants.CONTINUE)
        set(value) = mPrefs.edit().putString(prefsOnListEnded, value).apply()

    var isCovers: Boolean
        get() = mPrefs.getBoolean(prefsCover, false)
        set(value) = mPrefs.edit().putBoolean(prefsCover, value).apply()

    var songsVisualization
        get() = mPrefs.getString(prefsSongsVisual, GoConstants.TITLE)
        set(value) = mPrefs.edit().putString(prefsSongsVisual, value.toString()).apply()

    var artistsSorting
        get() = mPrefs.getInt(prefsArtistsSorting, GoConstants.DESCENDING_SORTING)
        set(value) = mPrefs.edit().putInt(prefsArtistsSorting, value).apply()

    var foldersSorting
        get() = mPrefs.getInt(prefsFoldersSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit().putInt(prefsFoldersSorting, value).apply()

    var albumsSorting
        get() = mPrefs.getInt(prefsAlbumsSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit().putInt(prefsAlbumsSorting, value).apply()

    var allMusicSorting
        get() = mPrefs.getInt(prefsAllMusicSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit().putInt(prefsAllMusicSorting, value).apply()

    var filters: Set<String>?
        get() = mPrefs.getStringSet(prefsFilter, setOf())
        set(value) = mPrefs.edit().putStringSet(prefsFilter, value).apply()

    var fastSeekingStep
        get() = mPrefs.getInt(prefsFastSeek, 5)
        set(value) = mPrefs.edit().putInt(prefsFastSeek, value).apply()

    var isFastSeekingActions: Boolean
        get() = mPrefs.getBoolean(prefsFastSeekActions, false)
        set(value) = mPrefs.edit().putBoolean(prefsFastSeekActions, value).apply()

    var isPreciseVolumeEnabled
        get() = mPrefs.getBoolean(prefsPreciseVolume, false)
        set(value) = mPrefs.edit().putBoolean(prefsPreciseVolume, value).apply()

    var isFocusEnabled
        get() = mPrefs.getBoolean(prefsFocus, true)
        set(value) = mPrefs.edit().putBoolean(prefsFocus, value).apply()

    var isHeadsetPlugEnabled
        get() = mPrefs.getBoolean(prefsHeadsetPlug, true)
        set(value) = mPrefs.edit().putBoolean(prefsHeadsetPlug, value).apply()

    // Retrieve object from the Preferences using Moshi
    private fun <T : Any> putObjectForType(key: String, value: T?, type: Type) {
        val json = mMoshi.adapter<T>(type).toJson(value)
        mPrefs.edit().putString(key, json).apply()
    }

    private fun <T : Any> getObjectForType(key: String, type: Type): T? {
        mPrefs.getString(key, null)?.let { json ->
            return mMoshi.adapter<T>(type).fromJson(json)
        }
        return null
    }

    // Saves object into the Preferences using Moshi
    private fun <T : Any> putObjectForClass(key: String, value: T?, clazz: Class<T>) {
        val json = mMoshi.adapter(clazz).toJson(value)
        mPrefs.edit().putString(key, json).apply()
    }

    private fun <T : Any> getObjectForClass(key: String, clazz: Class<T>): T? {
        mPrefs.getString(key, null)?.let { json ->
            return mMoshi.adapter(clazz).fromJson(json)
        }
        return null
    }
}

