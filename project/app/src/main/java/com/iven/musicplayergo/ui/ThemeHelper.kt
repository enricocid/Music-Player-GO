package com.iven.musicplayergo.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat

object ThemeHelper {

    val LIGHT_MODE = "light"
    val DARK_MODE = "dark"
    val DEFAULT_MODE = "default"

    @JvmStatic
    fun applyTheme(themePref: String) {

        when (themePref) {
            LIGHT_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK_MODE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> {
                val mode =
                    if (BuildCompat.isAtLeastQ()) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }
}
