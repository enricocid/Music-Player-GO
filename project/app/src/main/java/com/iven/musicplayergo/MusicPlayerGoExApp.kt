package com.iven.musicplayergo

import android.app.Application

val musicRepo: MusicRepo by lazy {
    MusicPlayerGoExApp.musicRepo
}

class MusicPlayerGoExApp : Application() {
    companion object {
        lateinit var musicRepo: MusicRepo
    }

    override fun onCreate() {
        musicRepo = MusicRepo()
        super.onCreate()
    }
}
