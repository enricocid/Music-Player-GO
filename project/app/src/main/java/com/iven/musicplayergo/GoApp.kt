package com.iven.musicplayergo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.iven.musicplayergo.utils.Theming

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
        AppCompatDelegate.setDefaultNightMode(Theming.getDefaultNightMode(applicationContext))
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .diskCachePolicy(CachePolicy.DISABLED)
        .crossfade(true)
        .build()
}
