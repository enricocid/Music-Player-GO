package com.iven.musicplayergo

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.iven.musicplayergo.enums.SongsVisualOpts
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.SavedMusic
import java.lang.reflect.Type

class GoPreferences(context: Context) {

    private val prefsLatestVolume = context.getString(R.string.latest_volume_pref)
    private val prefsLatestPlayedSong = context.getString(R.string.latest_played_song_pref)
    private val prefsLovedSongs = context.getString(R.string.loved_songs_pref)

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDef = context.getString(R.string.theme_pref_light)
    private val prefsAccent = context.getString(R.string.accent_pref)
    private val prefsEdgeToEdge = context.getString(R.string.edge_pref)

    private val prefsActiveFragments = context.getString(R.string.active_fragments_pref)
    val prefsActiveFragmentsDef = setOf(0, 1, 2, 3, 4)

    private val prefsCover = context.getString(R.string.covers_pref)

    private val prefSongsVisual = context.getString(R.string.song_visual_pref)
    private val prefsSongVisualDef = SongsVisualOpts.TITLE.ordinal.toString()

    private val prefsArtistsSorting = context.getString(R.string.artists_sorting_pref)
    private val prefsFoldersSorting = context.getString(R.string.folders_sorting_pref)
    private val prefsAlbumsSorting = context.getString(R.string.albums_sorting_pref)

    private val prefsPreciseVolume = context.getString(R.string.precise_volume_pref)
    private val prefsFocus = context.getString(R.string.focus_pref)
    private val prefsHeadsetPlug = context.getString(R.string.headset_pref)

    private val prefsFilter = context.getString(R.string.filter_pref)

    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val mGson = GsonBuilder().create()

    // active fragments type
    private val typeActiveFragments = object : TypeToken<Set<Int>>() {}.type

    // last played song is a SavedMusic
    private val typeLastPlayedSong = object : TypeToken<SavedMusic>() {}.type

    //loved songs is a list of SavedMusic
    private val typeLovedSongs = object : TypeToken<MutableList<SavedMusic>>() {}.type

    var latestVolume: Int
        get() = mPrefs.getInt(prefsLatestVolume, 100)
        set(value) = mPrefs.edit().putInt(prefsLatestVolume, value).apply()

    var latestPlayedSong: SavedMusic?
        get() = getObject(
                prefsLatestPlayedSong,
                typeLastPlayedSong
        )
        set(value) = putObject(prefsLatestPlayedSong, value)

    var lovedSongs: MutableList<SavedMusic>?
        get() = getObject(
                prefsLovedSongs,
                typeLovedSongs
        )
        set(value) = putObject(prefsLovedSongs, value)

    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit().putString(prefsTheme, value).apply()

    var accent
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
        set(value) = mPrefs.edit().putInt(prefsAccent, value).apply()

    var isEdgeToEdge
        get() = mPrefs.getBoolean(
                prefsEdgeToEdge,
                false
        ) && VersioningHelper.isOreoMR1()
        set(value) = mPrefs.edit().putBoolean(prefsEdgeToEdge, value).apply()

    var activeFragments: Set<Int>
        get() = getObject(prefsActiveFragments, typeActiveFragments) ?: prefsActiveFragmentsDef
        set(value) = putObject(prefsActiveFragments, value)

    var isCovers: Boolean
        get() = mPrefs.getBoolean(prefsCover, false)
        set(value) = mPrefs.edit().putBoolean(prefsCover, value).apply()

    var songsVisualization
        get() = getSongVisualization()
        set(value) = mPrefs.edit().putString(prefSongsVisual, value.ordinal.toString()).apply()

    private fun getSongVisualization(): SongsVisualOpts {
        mPrefs.getString(prefSongsVisual, prefsSongVisualDef)?.let {
            return SongsVisualOpts.values()[Integer.parseInt(it)]
        }
        return SongsVisualOpts.FILE_NAME
    }

    var artistsSorting
        get() = mPrefs.getInt(prefsArtistsSorting, GoConstants.DESCENDING_SORTING)
        set(value) = mPrefs.edit().putInt(prefsArtistsSorting, value).apply()

    var foldersSorting
        get() = mPrefs.getInt(prefsFoldersSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit().putInt(prefsFoldersSorting, value).apply()

    var albumsSorting
        get() = mPrefs.getInt(prefsAlbumsSorting, GoConstants.DEFAULT_SORTING)
        set(value) = mPrefs.edit().putInt(prefsAlbumsSorting, value).apply()

    var filters: Set<String>?
        get() = mPrefs.getStringSet(prefsFilter, setOf())
        set(value) = mPrefs.edit().putStringSet(prefsFilter, value).apply()

    var isPreciseVolumeEnabled
        get() = mPrefs.getBoolean(prefsPreciseVolume, true)
        set(value) = mPrefs.edit().putBoolean(prefsPreciseVolume, value).apply()

    var isFocusEnabled
        get() = mPrefs.getBoolean(prefsFocus, true)
        set(value) = mPrefs.edit().putBoolean(prefsFocus, value).apply()

    var isHeadsetPlugEnabled
        get() = mPrefs.getBoolean(prefsHeadsetPlug, true)
        set(value) = mPrefs.edit().putBoolean(prefsHeadsetPlug, value).apply()

    /**
     * Saves object into the Preferences.
     * Only the fields are stored. Methods, Inner classes, Nested classes and inner interfaces are not stored.
     **/
    private fun <T> putObject(key: String, y: T) {
        //Convert object to JSON String.
        val inString = mGson.toJson(y)
        //Save that String in SharedPreferences
        mPrefs.edit().putString(key, inString).apply()
    }

    /**
     * Get object from the Preferences.
     **/
    private fun <T> getObject(key: String, t: Type): T? {
        //We read JSON String which was saved.
        val value = mPrefs.getString(key, null)

        //JSON String was found which means object can be read.
        //We convert this JSON String to model object. Parameter "c" (of type Class<T>" is used to cast.
        return mGson.fromJson(value, t)
    }
}

