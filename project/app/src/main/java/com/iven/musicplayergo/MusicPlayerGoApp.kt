package com.iven.musicplayergo

import android.app.Application
import com.iven.musicplayergo.uihelpers.MusicPlayerGoPreferences


val mMusicPlayerGoPreferences: MusicPlayerGoPreferences by lazy {
    MusicPlayerGoApp.prefs
}

class MusicPlayerGoApp : Application() {

    companion object {
        lateinit var prefs: MusicPlayerGoPreferences
    }

    override fun onCreate() {
        prefs = MusicPlayerGoPreferences(applicationContext)
        super.onCreate()
    }

}