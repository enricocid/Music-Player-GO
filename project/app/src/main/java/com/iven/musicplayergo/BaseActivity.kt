package com.iven.musicplayergo

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.iven.musicplayergo.preferences.ContextUtils
import com.iven.musicplayergo.utils.Theming
import java.util.*


abstract class BaseActivity: AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        if (Theming.isThemeBlack(resources)) {
            window?.statusBarColor = Color.BLACK
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { ctx ->
            // Be sure that prefs are initialized
            GoPreferences.initPrefs(newBase).locale?.run {
                val locale = Locale.forLanguageTag(this)
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, locale)
                super.attachBaseContext(localeUpdatedContext)
                return
            }
            super.attachBaseContext(ContextUtils.updateLocale(ctx, Locale.getDefault()))
        }
    }
}
