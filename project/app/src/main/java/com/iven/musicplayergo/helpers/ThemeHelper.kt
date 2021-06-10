package com.iven.musicplayergo.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
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
            GoConstants.SHUFFLE_SORTING -> R.drawable.ic_shuffle
            GoConstants.ASCENDING_SORTING -> R.drawable.ic_sort_alphabetical_descending
            GoConstants.DESCENDING_SORTING -> R.drawable.ic_sort_alphabetical_ascending
            GoConstants.TRACK_SORTING -> R.drawable.ic_sort_numeric_descending
            else -> R.drawable.ic_sort_numeric_ascending
        }
    }

    fun getSortIconForSongsDisplayName(sort: Int) : Int {
        return when (sort) {
            GoConstants.SHUFFLE_SORTING -> R.drawable.ic_shuffle
            GoConstants.ASCENDING_SORTING -> R.drawable.ic_sort_alphabetical_descending
            else -> R.drawable.ic_sort_alphabetical_ascending
        }
    }

    @JvmStatic
    fun isDeviceLand(resources: Resources) =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Fixed array of pairs (first: accent, second: theme, third: color primary dark)
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
            Pair(R.color.deep_orange, R.style.BaseTheme_DeepOrange),
            Pair(R.color.brown, R.style.BaseTheme_Brown),
            Pair(R.color.grey, R.style.BaseTheme_Grey),
            Pair(R.color.blue_grey, R.style.BaseTheme_BlueGrey),
            Pair(R.color.red_300, R.style.BaseTheme_Red300),
            Pair(R.color.pink_300, R.style.BaseTheme_Pink300),
            Pair(R.color.purple_300, R.style.BaseTheme_Purple300),
            Pair(R.color.deep_purple_300, R.style.BaseTheme_DeepPurple300),
            Pair(R.color.indigo_300, R.style.BaseTheme_Indigo300),
            Pair(R.color.blue_300, R.style.BaseTheme_Blue300),
            Pair(R.color.light_blue_300, R.style.BaseTheme_LightBlue300),
            Pair(R.color.cyan_300, R.style.BaseTheme_Cyan300),
            Pair(R.color.teal_300, R.style.BaseTheme_Teal300),
            Pair(R.color.green_300, R.style.BaseTheme_Green300),
            Pair(R.color.light_green_300, R.style.BaseTheme_LightGreen300),
            Pair(R.color.lime_300, R.style.BaseTheme_Lime300),
            Pair(R.color.amber_300, R.style.BaseTheme_Amber300),
            Pair(R.color.orange_300, R.style.BaseTheme_Orange300),
            Pair(R.color.deep_orange_300, R.style.BaseTheme_DeepOrange300),
            Pair(R.color.brown_300, R.style.BaseTheme_Brown300),
            Pair(R.color.blue_grey_300, R.style.BaseTheme_BlueGrey300)
    )

    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun getAccentName(context: Context, accent: Int): Spanned {
        val accentName = context.resources.getResourceEntryName(accent).replace(
                context.getString(R.string.underscore_delimiter),
                context.getString(R.string.space_delimiter)
        ).replaceFirstChar { it.uppercase() }
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
        ).replaceFirstChar { it.uppercase() }

    // Search theme from accents array of Pair, returns a Pair(theme, position)
    @JvmStatic
    fun getAccentedTheme() = try {
        val pair = accents.find { (first) -> first == goPreferences.accent }
        val theme = pair!!.second
        val position = accents.indexOf(pair)
        Pair(theme, position)
    } catch (e: Exception) {
        Pair(R.style.BaseTheme_DeepPurple, 3)
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
        else -> R.drawable.ic_repeat_one_notif_disabled
    }
}
