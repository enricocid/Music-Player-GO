package com.iven.musicplayergo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.iven.musicplayergo.helpers.ThemeHelper

val goPreferences: MusicPlayerGoPreferences by lazy {
    MusicPlayerGoApp.prefs
}

class MusicPlayerGoApp : Application() {

    companion object {
        lateinit var prefs: MusicPlayerGoPreferences
    }

    override fun onCreate() {
        super.onCreate()
        prefs = MusicPlayerGoPreferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(ThemeHelper.getDefaultNightMode(applicationContext))
    }
}
