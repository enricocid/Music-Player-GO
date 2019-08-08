package com.iven.musicplayergo

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class MusicPlayerGoPreferences(context: Context) {

    private val prefsAccent = context.getString(R.string.prefs_accent)
    private val prefsTheme = context.getString(R.string.prefs_theme)
    private val prefsSearchBar = context.getString(R.string.prefs_search_bar)

    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var accent: Int
        get() = mPrefs.getInt(prefsAccent, R.color.blue)
        set(value) = mPrefs.edit().putInt(prefsAccent, value).apply()

    var isThemeInverted: Boolean
        get() = mPrefs.getBoolean(prefsTheme, false)
        set(value) = mPrefs.edit().putBoolean(prefsTheme, value).apply()

    var isSearchBarEnabled: Boolean
        get() = mPrefs.getBoolean(prefsSearchBar, false)
        set(value) = mPrefs.edit().putBoolean(prefsSearchBar, value).apply()
}

