package com.iven.musicplayergo

import android.app.Application
import com.iven.musicplayergo.music.MusicRepo

val musicRepo: MusicRepo by lazy {
    MusicPlayerGoExApp.musicRepo
}

val musicPlayerGoExAppPreferences: MusicPlayerGoExPreferences by lazy {
    MusicPlayerGoExApp.prefs
}

class MusicPlayerGoExApp : Application() {
    companion object {
        lateinit var prefs: MusicPlayerGoExPreferences
        lateinit var musicRepo: MusicRepo
    }

    override fun onCreate() {
        prefs = MusicPlayerGoExPreferences(applicationContext)
        musicRepo = MusicRepo()
        super.onCreate()
    }
}
