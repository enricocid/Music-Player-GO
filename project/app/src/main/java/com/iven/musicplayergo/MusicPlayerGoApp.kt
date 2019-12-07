package com.iven.musicplayergo

import android.app.Application
import com.iven.musicplayergo.music.MusicLibrary

val goPreferences: MusicPlayerGoPreferences by lazy {
    MusicPlayerGoApp.prefs
}

val musicLibrary: MusicLibrary by lazy {
    MusicPlayerGoApp.musicLibrary
}

class MusicPlayerGoApp : Application() {

    companion object {
        lateinit var prefs: MusicPlayerGoPreferences
        lateinit var musicLibrary: MusicLibrary
    }

    override fun onCreate() {
        prefs = MusicPlayerGoPreferences(applicationContext)
        musicLibrary = MusicLibrary()
        super.onCreate()
    }
}
