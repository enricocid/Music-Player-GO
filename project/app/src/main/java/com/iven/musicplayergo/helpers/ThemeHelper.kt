package com.iven.musicplayergo.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.text.Spanned
import android.util.TypedValue
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.widget.ImageViewCompat
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.MainActivity


object ThemeHelper {

    @JvmStatic
    fun applyChanges(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        val bundle = bundleOf(GoConstants.RESTORE_SETTINGS_FRAGMENT to true)
        intent.putExtras(bundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        with(activity) {
            finishAfterTransition()
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @JvmStatic
    fun getDefaultNightMode(context: Context) = when (goPreferences.theme) {
        context.getString(R.string.theme_pref_light) -> AppCompatDelegate.MODE_NIGHT_NO
        context.getString(R.string.theme_pref_dark) -> AppCompatDelegate.MODE_NIGHT_YES
        else -> if (VersioningHelper.isQ()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
    }

    @JvmStatic
    fun resolveThemeIcon(context: Context) = when (goPreferences.theme) {
        context.getString(R.string.theme_pref_light) -> R.drawable.ic_day
        context.getString(R.string.theme_pref_auto) -> R.drawable.ic_auto
        else -> R.drawable.ic_night
    }

    fun getSortIconForSongs(sort: Int): Int {
        return when (sort) {
            GoConstants.ASCENDING_SORTING -> R.drawable.ic_sort_alphabetical_descending
            GoConstants.DESCENDING_SORTING -> R.drawable.ic_sort_alphabetical_ascending
            GoConstants.TRACK_SORTING -> R.drawable.ic_sort_numeric_descending
            else -> R.drawable.ic_sort_numeric_ascending
        }
    }

    fun getSortIconForSongsDisplayName(sort: Int) : Int {
        return when (sort) {
            GoConstants.ASCENDING_SORTING -> R.drawable.ic_sort_alphabetical_descending
            else -> R.drawable.ic_sort_alphabetical_ascending
        }
    }

    @JvmStatic
    fun isDeviceLand(resources: Resources) =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Fixed array of pairs (first: accent, second: theme)
    @JvmStatic
    val accents = arrayOf(
        Pair(first = R.color.red, second = R.style.BaseTheme_Red),
        Pair(first = R.color.pink, second = R.style.BaseTheme_Pink),
        Pair(first = R.color.purple, second = R.style.BaseTheme_Purple),
        Pair(first = R.color.deep_purple, second = R.style.BaseTheme_DeepPurple),
        Pair(first = R.color.indigo, second = R.style.BaseTheme_Indigo),
        Pair(first = R.color.blue, second = R.style.BaseTheme_Blue),
        Pair(first = R.color.light_blue, second = R.style.BaseTheme_LightBlue),
        Pair(first = R.color.cyan, second = R.style.BaseTheme_Cyan),
        Pair(first = R.color.teal, second = R.style.BaseTheme_Teal),
        Pair(first = R.color.green, second = R.style.BaseTheme_Green),
        Pair(first = R.color.light_green, second = R.style.BaseTheme_LightGreen),
        Pair(first = R.color.lime, second = R.style.BaseTheme_Lime),
        Pair(first = R.color.yellow, second = R.style.BaseTheme_Yellow),
        Pair(first = R.color.amber, second = R.style.BaseTheme_Amber),
        Pair(first = R.color.orange, second = R.style.BaseTheme_Orange),
        Pair(first = R.color.deep_orange, second = R.style.BaseTheme_DeepOrange),
        Pair(first = R.color.brown, second = R.style.BaseTheme_Brown),
        Pair(first = R.color.grey, second = R.style.BaseTheme_Grey),
        Pair(first = R.color.blue_grey, second = R.style.BaseTheme_BlueGrey),
        Pair(first = R.color.red_200, second = R.style.BaseTheme_Red200),
        Pair(first = R.color.pink_200, second = R.style.BaseTheme_Pink200),
        Pair(first = R.color.purple_200, second = R.style.BaseTheme_Purple200),
        Pair(first = R.color.deep_purple_200, second = R.style.BaseTheme_DeepPurple200),
        Pair(first = R.color.indigo_200, second = R.style.BaseTheme_Indigo200),
        Pair(first = R.color.blue_200, second = R.style.BaseTheme_Blue200),
        Pair(first = R.color.light_blue_200, second = R.style.BaseTheme_LightBlue200),
        Pair(first = R.color.cyan_200, second = R.style.BaseTheme_Cyan200),
        Pair(first = R.color.teal_200, second = R.style.BaseTheme_Teal200),
        Pair(first = R.color.green_200, second = R.style.BaseTheme_Green200),
        Pair(first = R.color.light_green_200, second = R.style.BaseTheme_LightGreen200),
        Pair(first = R.color.lime_200, second = R.style.BaseTheme_Lime200),
        Pair(first = R.color.amber_200, second = R.style.BaseTheme_Amber200),
        Pair(first = R.color.orange_200, second = R.style.BaseTheme_Orange200),
        Pair(first = R.color.deep_orange_200, second = R.style.BaseTheme_DeepOrange200),
        Pair(first = R.color.brown_200, second = R.style.BaseTheme_Brown200),
        Pair(first = R.color.blue_grey_200, second = R.style.BaseTheme_BlueGrey200)
    )

    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun getAccentName(context: Context, accent: Int): Spanned {
        val accentName = context.resources.getResourceEntryName(accent).replace(
            context.getString(R.string.underscore_delimiter),
            context.getString(R.string.space_delimiter)
        ).replaceFirstChar(Char::uppercase)
        return context.getString(
            R.string.accent_and_hex,
            accentName,
            context.getString(accent).uppercase()
        ).parseAsHtml()
    }

    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun getAccentNameForPref(context: Context, accent: Int) =
        context.resources.getResourceEntryName(accent).replace(
            context.getString(R.string.underscore_delimiter),
            context.getString(R.string.space_delimiter)
        ).replaceFirstChar(Char::uppercase)

    // Search theme from accents array of Pair, returns a Pair(theme, position)
    @JvmStatic
    fun getAccentedTheme() = try {
        val pair = accents.find { (first) -> first == goPreferences.accent }
        val theme = pair!!.second
        val position = accents.indexOf(pair)
        Pair(first = theme, second = position)
    } catch (e: Exception) {
        Pair(first = R.style.BaseTheme_DeepPurple, second = 3)
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
        var accent = goPreferences.accent

        // Fallback to default color when the pref is f@#$ed (when resources change)
        if (!accents.map { accentId -> accentId.first }.contains(accent)) {
            accent = R.color.deep_purple
            goPreferences.accent = accent
        }
        return ContextCompat.getColor(context, accent)
    }

    @ColorInt
    @JvmStatic
    fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
        val resolvedAttr: TypedValue =
            resolveThemeAttr(
                    context,
                    colorAttr
            )
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        val colorRes =
            if (resolvedAttr.resourceId != 0) {
                resolvedAttr.resourceId
            } else {
                resolvedAttr.data
            }
        return ContextCompat.getColor(context, colorRes)
    }

    @JvmStatic
    private fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int) =
        TypedValue().apply { context.theme.resolveAttribute(attrRes, this, true) }

    @JvmStatic
    fun getAlphaAccent(context: Context) : Int {
        val accent = goPreferences.accent
        var alpha = if (accent == R.color.yellow) {
            200
        } else {
            150
        }
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (uiMode == Configuration.UI_MODE_NIGHT_YES) {
            alpha = 150
        }
        return ColorUtils.setAlphaComponent(resolveThemeAccent(context), alpha)
    }

    @JvmStatic
    fun getTabIcon(tab: String) = when (tab) {
        GoConstants.ARTISTS_TAB -> R.drawable.ic_artist
        GoConstants.ALBUM_TAB -> R.drawable.ic_library_music
        GoConstants.SONGS_TAB -> R.drawable.ic_music_note
        GoConstants.FOLDERS_TAB -> R.drawable.ic_folder_music
        else -> R.drawable.ic_settings
    }

    @JvmStatic
    fun getPreciseVolumeIcon(volume: Int) = when (volume) {
        in 1..33 -> R.drawable.ic_volume_mute
        in 34..67 -> R.drawable.ic_volume_down
        in 68..100 -> R.drawable.ic_volume_up
        else -> R.drawable.ic_volume_off
    }

    @JvmStatic
    fun getRepeatIcon(mediaPlayerHolder: MediaPlayerHolder) = when {
        mediaPlayerHolder.isRepeat1X -> R.drawable.ic_repeat_one
        mediaPlayerHolder.isLooping -> R.drawable.ic_repeat
        mediaPlayerHolder.isPauseOnEnd -> R.drawable.ic_pause
        else -> R.drawable.ic_repeat_one_notif_disabled
    }
}
