package com.iven.musicplayergo.uihelpers

import android.content.Context
import com.iven.musicplayergo.R

class MusicPlayerGoPreferences(context: Context) {

    private val prefsAccent = context.getString(R.string.prefs_accent)
    private val prefsAccentValue = context.getString(R.string.prefs_accent_value)
    private val prefsTheme = context.getString(R.string.prefs_theme)
    private val prefsThemeValue = context.getString(R.string.prefs_theme_value)
    private val prefsSearchBar = context.getString(R.string.prefs_search_bar)
    private val prefsSearchBarValue = context.getString(R.string.prefs_search_bar_value)

    private val accentPref = context.getSharedPreferences(prefsAccent, Context.MODE_PRIVATE)
    private val themePref = context.getSharedPreferences(prefsTheme, Context.MODE_PRIVATE)
    private val searchBarPref = context.getSharedPreferences(prefsSearchBar, Context.MODE_PRIVATE)

    var accent: Int
        get() = accentPref.getInt(prefsAccentValue, R.color.blue)
        set(value) = accentPref.edit().putInt(prefsAccentValue, value).apply()

    var isThemeInverted: Boolean
        get() = themePref.getBoolean(prefsTheme, false)
        set(value) = themePref.edit().putBoolean(prefsThemeValue, value).apply()

    var isSearchBarEnabled: Boolean
        get() = searchBarPref.getBoolean(prefsSearchBar, false)
        set(value) = searchBarPref.edit().putBoolean(prefsSearchBarValue, value).apply()

    //get theme
    fun resolveTheme(isThemeDark: Boolean, accent: Int?): Int {

        return when (accent) {

            R.color.red -> if (isThemeDark) R.style.AppThemeRedInverted else R.style.AppThemeRed

            R.color.pink -> if (isThemeDark) R.style.AppThemePinkInverted else R.style.AppThemePink

            R.color.purple ->
                if (isThemeDark) R.style.AppThemePurpleInverted else R.style.AppThemePurple

            R.color.deep_purple ->
                if (isThemeDark) R.style.AppThemeDeepPurpleInverted else R.style.AppThemeDeepPurple

            R.color.indigo ->
                if (isThemeDark) R.style.AppThemeIndigoInverted else R.style.AppThemeIndigo

            R.color.blue -> if (isThemeDark) R.style.AppThemeBlueInverted else R.style.AppThemeBlue

            R.color.light_blue ->
                if (isThemeDark) R.style.AppThemeLightBlueInverted else R.style.AppThemeLightBlue

            R.color.cyan -> if (isThemeDark) R.style.AppThemeCyanInverted else R.style.AppThemeCyan

            R.color.teal -> if (isThemeDark) R.style.AppThemeTealInverted else R.style.AppThemeTeal

            R.color.green -> if (isThemeDark) R.style.AppThemeGreenInverted else R.style.AppThemeGreen

            R.color.amber -> if (isThemeDark) R.style.AppThemeAmberInverted else R.style.AppThemeAmber

            R.color.orange ->
                if (isThemeDark) R.style.AppThemeOrangeInverted else R.style.AppThemeOrange

            R.color.deep_orange ->
                if (isThemeDark) R.style.AppThemeDeepOrangeInverted else R.style.AppThemeDeepOrange

            R.color.brown -> if (isThemeDark) R.style.AppThemeBrownInverted else R.style.AppThemeBrown

            R.color.gray ->
                if (isThemeDark) R.style.AppThemeGrayLightInverted else R.style.AppThemeGrayLight

            R.color.blue_gray ->
                if (isThemeDark) R.style.AppThemeBlueGrayInverted else R.style.AppThemeBlueGray

            else -> R.color.blue
        }
    }
}

