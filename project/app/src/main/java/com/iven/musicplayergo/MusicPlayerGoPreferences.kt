package com.iven.musicplayergo

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class MusicPlayerGoPreferences(context: Context) {

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDefault = context.getString(R.string.theme_pref_light)
    private val prefsAccent = context.getString(R.string.accent_pref)

    private val prefsFastScroll = context.getString(R.string.fast_scroll_pref)
    private val prefsSearchBar = context.getString(R.string.search_bar_pref)
    private val prefsArtistsSorting = context.getString(R.string.artists_sorting_pref)
    private val prefsHiddenArtistsFolders = context.getString(R.string.hidden_items_pref)
    private val prefsFocus = context.getString(R.string.focus_pref)
    private val prefsHeadsetPlug = context.getString(R.string.headset_pref)

    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var theme: String?
        get() = mPrefs.getString(prefsTheme, prefsThemeDefault)
        set(value) = mPrefs.edit().putString(prefsTheme, value).apply()

    var accent: Int
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
        set(value) = mPrefs.edit().putInt(prefsAccent, value).apply()

    var isFastScrollEnabled: Boolean
        get() = mPrefs.getBoolean(prefsFastScroll, true)
        set(value) = mPrefs.edit().putBoolean(prefsFastScroll, value).apply()

    var isSearchBarEnabled: Boolean
        get() = mPrefs.getBoolean(prefsSearchBar, true)
        set(value) = mPrefs.edit().putBoolean(prefsSearchBar, value).apply()

    var artistsSorting: Int
        get() = mPrefs.getInt(prefsArtistsSorting, R.id.ascending_sorting)
        set(value) = mPrefs.edit().putInt(prefsArtistsSorting, value).apply()

    var hiddenItems: Set<String>?
        get() = mPrefs.getStringSet(prefsHiddenArtistsFolders, setOf())
        set(value) = mPrefs.edit().putStringSet(prefsHiddenArtistsFolders, value).apply()

    var isFocusEnabled: Boolean
        get() = mPrefs.getBoolean(prefsFocus, true)
        set(value) = mPrefs.edit().putBoolean(prefsFocus, value).apply()

    var isHeadsetPlugEnabled: Boolean
        get() = mPrefs.getBoolean(prefsHeadsetPlug, true)
        set(value) = mPrefs.edit().putBoolean(prefsHeadsetPlug, value).apply()
}

