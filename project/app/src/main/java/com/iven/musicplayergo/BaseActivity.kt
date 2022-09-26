package com.iven.musicplayergo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.iven.musicplayergo.preferences.ContextUtils
import java.util.*


abstract class BaseActivity: AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { ctx ->
            // Be sure that prefs are initialized
            val goPreferences = GoPreferences.initPrefs(newBase)
            if (goPreferences.locale == null) {
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, Locale.getDefault())
                super.attachBaseContext(localeUpdatedContext)
            } else {
                val locale = Locale.forLanguageTag(goPreferences.locale!!)
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, locale)
                super.attachBaseContext(localeUpdatedContext)
            }
        }
    }
}
