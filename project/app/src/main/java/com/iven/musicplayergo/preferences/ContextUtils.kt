package com.iven.musicplayergo.preferences

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.iven.musicplayergo.R
import java.util.*

// https://medium.com/swlh/android-app-specific-language-change-programmatically-using-kotlin-d650a5392220
class ContextUtils(base: Context): ContextWrapper(base) {

    companion object {

        @Suppress("DEPRECATION")
        fun updateLocale(ctx: Context, localeToSwitchTo: Locale): ContextWrapper {
            var context = ctx
            val resources = context.resources
            val configuration = resources.configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(localeToSwitchTo)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else {
                configuration.locale = localeToSwitchTo
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context = context.createConfigurationContext(configuration)
            } else {
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }
            return ContextUtils(context)
        }

        fun getLocalesList(resources: Resources): Map<String, String> {
            val codes = resources.getStringArray(R.array.supported_locales)
            val names = resources.getStringArray(R.array.supported_locales_names)
            val localesMap = codes.zip(names).toMap()
            return localesMap.entries.sortedBy { it.value }.associate { it.toPair() }
        }
    }
}
