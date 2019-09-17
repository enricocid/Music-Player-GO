package com.iven.musicplayergo.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import com.iven.musicplayergo.MainActivity
import com.iven.musicplayergo.R

object ThemeHelper {

    //update theme
    @JvmStatic
    fun applyNewThemeSmoothly(activity: Activity) {
        //smoothly set app theme
        val intent = Intent(activity, MainActivity::class.java)
        activity.startActivity(intent)
        activity.finish()
    }

    @JvmStatic
    fun applyTheme(context: Context, themePref: String?) {

        when (themePref) {
            context.getString(R.string.theme_pref_light) -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
            context.getString(R.string.theme_pref_dark) -> AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
            else -> {
                val mode =
                    if (BuildCompat.isAtLeastQ()) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    //fixed array of pairs (first: accent, second: theme)
    @JvmStatic
    val accents = arrayOf(
        Pair(R.color.red, R.style.BaseTheme_Red),
        Pair(R.color.pink, R.style.BaseTheme_Pink),
        Pair(R.color.purple, R.style.BaseTheme_Purple),
        Pair(R.color.deepPurple, R.style.BaseTheme_DeepPurple),
        Pair(R.color.indigo, R.style.BaseTheme_Indigo),
        Pair(R.color.blue, R.style.BaseTheme_Blue),
        Pair(R.color.lightBlue, R.style.BaseTheme_LightBlue),
        Pair(R.color.cyan, R.style.BaseTheme_Cyan),
        Pair(R.color.teal, R.style.BaseTheme_Teal),
        Pair(R.color.green, R.style.BaseTheme_Green),
        Pair(R.color.lightGreen, R.style.BaseTheme_LightGreen),
        Pair(R.color.lime, R.style.BaseTheme_Lime),
        Pair(R.color.yellow, R.style.BaseTheme_Yellow),
        Pair(R.color.amber, R.style.BaseTheme_Amber),
        Pair(R.color.orange, R.style.BaseTheme_Orange),
        Pair(R.color.deepOrange, R.style.BaseTheme_DeepOrange),
        Pair(R.color.brown, R.style.BaseTheme_Brown),
        Pair(R.color.grey, R.style.BaseTheme_Grey),
        Pair(R.color.blueGrey, R.style.BaseTheme_BlueGrey)
    )

    //finds theme and its position in accents array and returns a pair(theme, position)
    @JvmStatic
    fun getAccent(accent: Int): Pair<Int, Int> {
        return try {
            val pair = accents.find { pair -> pair.first == accent }
            val theme = pair!!.second
            val position = accents.indexOf(pair)
            Pair(theme, position)
        } catch (e: Exception) {
            Pair(R.style.BaseTheme_DeepPurple, 3)
        }
    }

    @JvmStatic
    fun getColor(context: Context, color: Int, emergencyColor: Int): Int {
        return try {
            ContextCompat.getColor(context, color)
        } catch (e: Exception) {
            ContextCompat.getColor(context, emergencyColor)
        }
    }
}
