package com.iven.musicplayergo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.iven.musicplayergo.helpers.ThemeHelper

val goPreferences: GoPreferences by lazy {
    GoApp.prefs
}

class GoApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var prefs: GoPreferences
    }

    override fun onCreate() {
        super.onCreate()
        prefs = GoPreferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(ThemeHelper.getDefaultNightMode(applicationContext))
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .bitmapPoolingEnabled(false)
            .crossfade(true)
            .error(R.drawable.album_art)
            .build()
    }
}
