package com.iven.musicplayergo

import android.app.Application
import com.iven.musicplayergo.music.MusicLibrary

val musicLibrary: MusicLibrary by lazy {
    MusicPlayerGoExApp.musicLibrary
}

val musicPlayerGoExAppPreferences: MusicPlayerGoExPreferences by lazy {
    MusicPlayerGoExApp.prefs
}

class MusicPlayerGoExApp : Application() {
    companion object {
        lateinit var prefs: MusicPlayerGoExPreferences
        lateinit var musicLibrary: MusicLibrary
    }

    override fun onCreate() {
        prefs = MusicPlayerGoExPreferences(applicationContext)
        musicLibrary = MusicLibrary()
        super.onCreate()
    }
}
