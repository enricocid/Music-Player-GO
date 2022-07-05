package com.iven.musicplayergo.utils


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.ImageViewCompat
import com.google.android.material.appbar.MaterialToolbar
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.setIconTint
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.MainActivity


object Theming {

    @JvmStatic
    fun applyChanges(activity: Activity, restoreSettings: Boolean) {
        with(activity) {
            finishAfterTransition()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtras(bundleOf(
                GoConstants.RESTORE_SETTINGS_FRAGMENT to restoreSettings
            ))
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @JvmStatic
    fun getDefaultNightMode(context: Context) = when (GoPreferences.getPrefsInstance().theme) {
        context.getString(R.string.theme_pref_light) -> AppCompatDelegate.MODE_NIGHT_NO
        context.getString(R.string.theme_pref_dark) -> AppCompatDelegate.MODE_NIGHT_YES
        else -> if (Versioning.isQ()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
    }

    @JvmStatic
    fun isThemeNight(resources: Resources) : Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
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

    @JvmStatic
    fun getAccentName(resources: Resources, position: Int): String {
        val accent = resources.getStringArray(R.array.accent_names)
        return try {
            when {
                position <= 18 -> accent[position]
                else -> {
                    val expandedList = accent.toMutableList().apply {
                        addAll(accent)
                    }
                    val result = when {
                        position == resources.getIntArray(R.array.colors).size - 1 -> accent.last()
                        position >= 31 -> expandedList[position +1]
                        else -> expandedList[position]
                    }
                    resources.getString(R.string.accent_200, result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return resources.getString(R.string.error_eq)
        }
    }

    @JvmStatic
    fun updateIconTint(imageView: ImageView, tint: Int) {
        ImageViewCompat.setImageTintList(
            imageView, ColorStateList.valueOf(tint)
        )
    }

    @JvmStatic
    private val styles = arrayOf(
        R.style.BaseTheme_Red,
        R.style.BaseTheme_Pink,
        R.style.BaseTheme_Purple,
        R.style.BaseTheme_DeepPurple,
        R.style.BaseTheme_Indigo,
        R.style.BaseTheme_Blue,
        R.style.BaseTheme_LightBlue,
        R.style.BaseTheme_Cyan,
        R.style.BaseTheme_Teal,
        R.style.BaseTheme_Green,
        R.style.BaseTheme_LightGreen,
        R.style.BaseTheme_Lime,
        R.style.BaseTheme_Yellow,
        R.style.BaseTheme_Amber,
        R.style.BaseTheme_Orange,
        R.style.BaseTheme_DeepOrange,
        R.style.BaseTheme_Brown,
        R.style.BaseTheme_Grey,
        R.style.BaseTheme_BlueGrey,
        R.style.BaseTheme_Red200,
        R.style.BaseTheme_Pink200,
        R.style.BaseTheme_Purple200,
        R.style.BaseTheme_DeepPurple200,
        R.style.BaseTheme_Indigo200,
        R.style.BaseTheme_Blue200,
        R.style.BaseTheme_LightBlue200,
        R.style.BaseTheme_Cyan200,
        R.style.BaseTheme_Teal200,
        R.style.BaseTheme_Green200,
        R.style.BaseTheme_LightGreen200,
        R.style.BaseTheme_Lime200,
        R.style.BaseTheme_Amber200,
        R.style.BaseTheme_Orange200,
        R.style.BaseTheme_DeepOrange200,
        R.style.BaseTheme_Brown200,
        R.style.BaseTheme_BlueGrey200
    )

    @JvmStatic
    private val stylesBlack = arrayOf(
        R.style.BaseTheme_Black_Red,
        R.style.BaseTheme_Black_Pink,
        R.style.BaseTheme_Black_Purple,
        R.style.BaseTheme_Black_DeepPurple,
        R.style.BaseTheme_Black_Indigo,
        R.style.BaseTheme_Black_Blue,
        R.style.BaseTheme_Black_LightBlue,
        R.style.BaseTheme_Black_Cyan,
        R.style.BaseTheme_Black_Teal,
        R.style.BaseTheme_Black_Green,
        R.style.BaseTheme_Black_LightGreen,
        R.style.BaseTheme_Black_Lime,
        R.style.BaseTheme_Black_Yellow,
        R.style.BaseTheme_Black_Amber,
        R.style.BaseTheme_Black_Orange,
        R.style.BaseTheme_Black_DeepOrange,
        R.style.BaseTheme_Black_Brown,
        R.style.BaseTheme_Black_Grey,
        R.style.BaseTheme_Black_BlueGrey,
        R.style.BaseTheme_Black_Red200,
        R.style.BaseTheme_Black_Pink200,
        R.style.BaseTheme_Black_Purple200,
        R.style.BaseTheme_Black_DeepPurple200,
        R.style.BaseTheme_Black_Indigo200,
        R.style.BaseTheme_Black_Blue200,
        R.style.BaseTheme_Black_LightBlue200,
        R.style.BaseTheme_Black_Cyan200,
        R.style.BaseTheme_Black_Teal200,
        R.style.BaseTheme_Black_Green200,
        R.style.BaseTheme_Black_LightGreen200,
        R.style.BaseTheme_Black_Lime200,
        R.style.BaseTheme_Black_Amber200,
        R.style.BaseTheme_Black_Orange200,
        R.style.BaseTheme_Black_DeepOrange200,
        R.style.BaseTheme_Black_Brown200,
        R.style.BaseTheme_Black_BlueGrey200
    )

    @JvmStatic
    fun resolveTheme(context: Context): Int {
        val position = GoPreferences.getPrefsInstance().accent
        val stylesRes = if (isThemeNight(context.resources) && GoPreferences.getPrefsInstance().isBlackTheme) {
            stylesBlack
        } else {
            styles
        }
        return stylesRes[position]
    }

    @ColorInt
    @JvmStatic
    fun resolveThemeColor(resources: Resources): Int {
        val position = GoPreferences.getPrefsInstance().accent
        val colors = resources.getIntArray(R.array.colors)
        return colors[position]
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
    fun getAlbumCoverAlpha(context: Context): Int {
        return when {
            isThemeNight(context.resources) && GoPreferences.getPrefsInstance().isBlackTheme -> 25
            isThemeNight(context.resources) -> 15
            else -> 20
        }
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

    @JvmStatic
    fun tintSleepTimerMenuItem(tb: MaterialToolbar, isEnabled: Boolean) {
        tb.menu.findItem(R.id.sleeptimer).setIconTint(if (isEnabled) {
            resolveThemeColor(tb.resources)
        } else {
            ContextCompat.getColor(tb.context, R.color.widgetsColor)
        })
    }
}
