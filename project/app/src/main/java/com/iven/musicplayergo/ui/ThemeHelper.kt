package com.iven.musicplayergo.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.iven.musicplayergo.MainActivity
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences

object ThemeHelper {

    //update theme
    @JvmStatic
    fun applyNewThemeSmoothly(activity: Activity) {
        //smoothly set app theme
        Handler().postDelayed({
            Intent(activity, MainActivity::class.java).apply {
                activity.startActivity(this)
            }
            activity.finish()
        }, 250)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    @JvmStatic
    private fun isThemeNight(): Boolean {
        return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    }

    @JvmStatic
    fun handleEdgeToEdge(window: Window?, view: View) {

        window?.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        var flags =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        view.apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                    val systemBarsFlag =
                        if (isThemeNight()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    flags =
                        flags or systemBarsFlag
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    flags =
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                }
            }

            systemUiVisibility = flags
        }
    }

    //fixed array of pairs (first: accent, second: theme)
    @JvmStatic
    val accents = arrayOf(
        Pair(R.color.red, R.style.BaseTheme_Red),
        Pair(R.color.pink, R.style.BaseTheme_Pink),
        Pair(R.color.purple, R.style.BaseTheme_Purple),
        Pair(R.color.deep_purple, R.style.BaseTheme_DeepPurple),
        Pair(R.color.indigo, R.style.BaseTheme_Indigo),
        Pair(R.color.blue, R.style.BaseTheme_Blue),
        Pair(R.color.light_blue, R.style.BaseTheme_LightBlue),
        Pair(R.color.cyan, R.style.BaseTheme_Cyan),
        Pair(R.color.teal, R.style.BaseTheme_Teal),
        Pair(R.color.green, R.style.BaseTheme_Green),
        Pair(R.color.light_green, R.style.BaseTheme_LightGreen),
        Pair(R.color.lime, R.style.BaseTheme_Lime),
        Pair(R.color.yellow, R.style.BaseTheme_Yellow),
        Pair(R.color.amber, R.style.BaseTheme_Amber),
        Pair(R.color.orange, R.style.BaseTheme_Orange),
        Pair(R.color.deepOrange, R.style.BaseTheme_DeepOrange),
        Pair(R.color.brown, R.style.BaseTheme_Brown),
        Pair(R.color.grey, R.style.BaseTheme_Grey),
        Pair(R.color.blue_grey, R.style.BaseTheme_BlueGrey)
    )

    //finds theme and its position in accents array and returns a pair(theme, position)
    @JvmStatic
    fun getAccentedTheme(): Pair<Int, Int> {
        return try {
            val pair = accents.find { pair -> pair.first == goPreferences.accent }
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

    @JvmStatic
    fun updateIconTint(imageView: ImageView, tint: Int) {
        ImageViewCompat.setImageTintList(
            imageView, ColorStateList.valueOf(tint)
        )
    }

    @ColorInt
    @JvmStatic
    fun resolveThemeAccent(context: Context): Int {
        return getColor(context, goPreferences.accent, R.color.deep_purple)
    }

    @ColorInt
    @JvmStatic
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr: TypedValue = resolveThemeAttr(context, colorAttr)
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        val colorRes =
            if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
        return ContextCompat.getColor(context, colorRes)
    }

    @JvmStatic
    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
        return TypedValue().apply { context.theme.resolveAttribute(attrRes, this, true) }
    }

    @JvmStatic
    fun getRecyclerViewDivider(context: Context): DividerItemDecoration {
        return DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        ).apply {
            setDrawable(
                ColorDrawable(
                    getAlphaAccent(
                        context,
                        if (isThemeNight()) 45 else 85
                    )
                )
            )
        }
    }

    @JvmStatic
    fun getAlphaAccent(context: Context, alpha: Int): Int {
        return ColorUtils.setAlphaComponent(resolveThemeAccent(context), alpha)
    }

    @JvmStatic
    fun getTabIcon(iconIndex: Int): Int {
        return when (iconIndex) {
            0 -> R.drawable.ic_person
            1 -> R.drawable.ic_music_note
            2 -> R.drawable.ic_folder
            else -> R.drawable.ic_more_horiz
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun buildSpanned(res: String): Spanned {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> Html.fromHtml(
                res,
                Html.FROM_HTML_MODE_LEGACY
            )
            else -> Html.fromHtml(res)
        }
    }
}
