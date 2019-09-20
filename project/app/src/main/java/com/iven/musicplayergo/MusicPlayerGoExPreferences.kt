package com.iven.musicplayergo

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class MusicPlayerGoExPreferences(context: Context) {

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDefault = context.getString(R.string.theme_pref_light)
    private val prefsAccent = context.getString(R.string.accent_pref)
    private val prefsSearchBar = context.getString(R.string.search_bar_pref)

    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var theme: String?
        get() = mPrefs.getString(prefsTheme, prefsThemeDefault)
        set(value) = mPrefs.edit().putString(prefsTheme, value).apply()

    var accent: Int
        get() = mPrefs.getInt(prefsAccent, R.color.deepPurple)
        set(value) = mPrefs.edit().putInt(prefsAccent, value).apply()

    var isSearchBarEnabled: Boolean
        get() = mPrefs.getBoolean(prefsSearchBar, true)
        set(value) = mPrefs.edit().putBoolean(prefsSearchBar, value).apply()
}

