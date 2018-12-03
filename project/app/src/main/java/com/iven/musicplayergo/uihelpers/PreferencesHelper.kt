package com.iven.musicplayergo.uihelpers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.iven.musicplayergo.R

private const val TAG_ACCENT_PREF = "com.iven.musicplayergo.pref_accent"
private const val TAG_ACCENT_VALUE = "com.iven.musicplayergo.pref_accent_value"
private const val TAG_THEME_PREF = "com.iven.musicplayergo.pref_theme"
private const val TAG_THEME_VALUE = "com.iven.musicplayergo.pref_theme_value"
private const val TAG_SEARCH_BAR_PREF = "com.iven.musicplayergo.pref_search_bar"
private const val TAG_SEARCH_BAR_VALUE = "com.iven.musicplayergo.pref_search_bar_value"

class PreferencesHelper(private val activity: Activity) {

    var themeInverted: Boolean? = false
    var accent: Int? = R.color.blue

    private fun getPreference(key: String): SharedPreferences {
        return activity.getSharedPreferences(key, Context.MODE_PRIVATE)
    }

    fun invertTheme() {
        val value = !isThemeInverted()
        getPreference(TAG_THEME_PREF).edit().putBoolean(TAG_THEME_VALUE, value).apply()
        activity.recreate()
    }

    fun isThemeInverted(): Boolean {
        themeInverted = getPreference(TAG_THEME_PREF).getBoolean(TAG_THEME_VALUE, false)
        return themeInverted!!
    }

    fun applyTheme(accent: Int, isThemeInverted: Boolean) {
        val theme = resolveTheme(isThemeInverted, accent)
        activity.setTheme(theme)
    }

    //get theme
    private fun resolveTheme(isThemeDark: Boolean, accent: Int): Int {

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

    fun setThemeAccent(accent: Int) {
        getPreference(TAG_ACCENT_PREF).edit().putInt(TAG_ACCENT_VALUE, accent).apply()
        activity.recreate()
    }

    fun getAccent(): Int {
        accent = getPreference(TAG_ACCENT_PREF).getInt(
            TAG_ACCENT_VALUE,
            R.color.blue
        )
        return accent!!
    }

    fun setSearchToolbarVisibility(isVisible: Boolean) {
        getPreference(TAG_SEARCH_BAR_PREF).edit().putBoolean(TAG_SEARCH_BAR_VALUE, isVisible).apply()
    }

    fun isSearchBarEnabled(): Boolean {
        return getPreference(TAG_SEARCH_BAR_PREF).getBoolean(TAG_SEARCH_BAR_VALUE, true)
    }
}