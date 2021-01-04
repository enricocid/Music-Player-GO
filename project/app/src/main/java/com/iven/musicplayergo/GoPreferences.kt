package com.iven.musicplayergo

import android.content.Context
import androidx.core.content.edit
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
    private val typeActiveTabs = Types.newParameterizedType(List::class.java, String::class.java)

    //loved songs is a list of Music
    private val typeLovedSongs = Types.newParameterizedType(List::class.java, Music::class.java)

    var latestVolume: Int
        get() = mPrefs.getInt(prefsLatestVolume, 100)
        set(value) = mPrefs.edit { putInt(prefsLatestVolume, value) }

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

    var lovedSongs: MutableList<Music>?
        get() = getObjectForType(
            prefsLovedSongs,
            typeLovedSongs
        )
        set(value) = putObjectForType(prefsLovedSongs, value, typeLovedSongs)

    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit { putString(prefsTheme, value) }

    var accent
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
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
        get() = mPrefs.getString(prefsSongsVisual, GoConstants.TITLE)
        set(value) = mPrefs.edit { putString(prefsSongsVisual, value.toString()) }

    var artistsSorting
        get() = mPrefs.getInt(prefsArtistsSorting, GoConstants.DESCENDING_SORTING)
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

    var filters: Set<String>?
        get() = mPrefs.getStringSet(prefsFilter, setOf())
        set(value) = mPrefs.edit { putStringSet(prefsFilter, value) }

    var fastSeekingStep
        get() = mPrefs.getInt(prefsFastSeek, 5)
        set(value) = mPrefs.edit { putInt(prefsFastSeek, value) }

    var isFastSeekingActions: Boolean
        get() = mPrefs.getBoolean(prefsFastSeekActions, false)
        set(value) = mPrefs.edit { putBoolean(prefsFastSeekActions, value) }

    var isPreciseVolumeEnabled
        get() = mPrefs.getBoolean(prefsPreciseVolume, false)
        set(value) = mPrefs.edit { putBoolean(prefsPreciseVolume, value) }

    var isFocusEnabled
        get() = mPrefs.getBoolean(prefsFocus, true)
        set(value) = mPrefs.edit { putBoolean(prefsFocus, value) }

    var isHeadsetPlugEnabled
        get() = mPrefs.getBoolean(prefsHeadsetPlug, true)
        set(value) = mPrefs.edit { putBoolean(prefsHeadsetPlug, value) }

    // Retrieve object from the Preferences using Moshi
    private fun <T : Any> putObjectForType(key: String, value: T?, type: Type) {
        val json = mMoshi.adapter<T>(type).toJson(value)
        mPrefs.edit { putString(key, json) }
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
        mPrefs.edit { putString(key, json) }
    }

    private fun <T : Any> getObjectForClass(key: String, clazz: Class<T>): T? {
        mPrefs.getString(key, null)?.let { json ->
            return mMoshi.adapter(clazz).fromJson(json)
        }
        return null
    }
}

