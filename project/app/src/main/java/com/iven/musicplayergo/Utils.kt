package com.iven.musicplayergo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.iven.musicplayergo.adapters.ArtistsAdapter
import com.reddit.indicatorfastscroll.FastScrollerView

object Utils {

    @JvmStatic
    fun getColor(context: Context, color: Int, emergencyColor: Int): Int {
        return try {
            ContextCompat.getColor(context, color)
        } catch (e: Exception) {
            ContextCompat.getColor(context, emergencyColor)
        }
    }

    @JvmStatic
    fun setupSearch(
        searchView: SearchView,
        artistsAdapter: ArtistsAdapter,
        artists: List<String>,
        indicator: FastScrollerView
    ) {

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override
            fun onQueryTextChange(newText: String): Boolean {
                processQuery(newText, artistsAdapter, artists)
                return false
            }

            override
            fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _: View, hasFocus: Boolean ->
            indicator.visibility = if (hasFocus) View.GONE else View.VISIBLE
            if (!hasFocus) searchView.isIconified = true
        }
    }

    @JvmStatic
    private fun processQuery(query: String, artistsAdapter: ArtistsAdapter, artists: List<String>) {
        // in real app you'd have it instantiated just once
        val results = mutableListOf<String>()

        try {
            // case insensitive search
            artists.iterator().forEach {
                if (it.toLowerCase().startsWith(query.toLowerCase())) {
                    results.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.size > 0) {
            artistsAdapter.setQueryResults(results)
        }
    }

    //get theme
    @JvmStatic
    fun resolveTheme(isThemeDark: Boolean, accent: Int?): Int {

        return when (accent) {

            R.color.red -> if (isThemeDark) R.style.AppThemeRedInverted else R.style.AppThemeRed

            R.color.pink -> if (isThemeDark) R.style.AppThemePinkInverted else R.style.AppThemePink

            R.color.purple ->
                if (isThemeDark) R.style.AppThemePurpleInverted else R.style.AppThemePurple

            R.color.deep_purple ->
                if (isThemeDark) R.style.AppThemeDeepPurpleInverted else R.style.AppThemeDeepPurple

            R.color.indigo ->
                if (isThemeDark) R.style.AppThemeIndigoInverted else R.style.AppThemeIndigo

            R.color.blue -> if (isThemeDark) R.style.AppThemeBlueInverted else R.style.AppThemeBlue

            R.color.light_blue ->
                if (isThemeDark) R.style.AppThemeLightBlueInverted else R.style.AppThemeLightBlue

            R.color.cyan -> if (isThemeDark) R.style.AppThemeCyanInverted else R.style.AppThemeCyan

            R.color.teal -> if (isThemeDark) R.style.AppThemeTealInverted else R.style.AppThemeTeal

            R.color.green -> if (isThemeDark) R.style.AppThemeGreenInverted else R.style.AppThemeGreen

            R.color.amber -> if (isThemeDark) R.style.AppThemeAmberInverted else R.style.AppThemeAmber

            R.color.orange ->
                if (isThemeDark) R.style.AppThemeOrangeInverted else R.style.AppThemeOrange

            R.color.deep_orange ->
                if (isThemeDark) R.style.AppThemeDeepOrangeInverted else R.style.AppThemeDeepOrange

            R.color.brown -> if (isThemeDark) R.style.AppThemeBrownInverted else R.style.AppThemeBrown

            R.color.gray ->
                if (isThemeDark) R.style.AppThemeGrayLightInverted else R.style.AppThemeGrayLight

            R.color.blue_gray ->
                if (isThemeDark) R.style.AppThemeBlueGrayInverted else R.style.AppThemeBlueGray

            else -> R.color.blue
        }
    }

    @JvmStatic
    fun setHorizontalScrollBehavior(parentView: View, vararg textViews: TextView) {
        var isLongPressed = false

        parentView.setOnLongClickListener {
            if (!isLongPressed) {
                textViews.iterator().forEach {
                    it.isSelected = true
                }
                isLongPressed = true
            }
            return@setOnLongClickListener true
        }

        parentView.setOnTouchListener { _, e ->
            if (isLongPressed && e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_OUTSIDE || e.action == MotionEvent.ACTION_MOVE) {

                textViews.iterator().forEach {
                    it.isSelected = false
                }
                isLongPressed = false
            }
            return@setOnTouchListener false
        }
    }

    //update theme
    @JvmStatic
    fun applyNewThemeSmoothly(activity: Activity) {
        //smoothly set app theme
        val intent = Intent(activity, MainActivity::class.java)
        activity.startActivity(intent)
        activity.finish()
    }

    @JvmStatic
    fun darkenColor(color: Int, factor: Float): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, factor)
    }

    @JvmStatic
    fun lightenColor(color: Int, factor: Float): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, factor)
    }

    @JvmStatic
    fun makeUnknownErrorToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.error_unknown), Toast.LENGTH_SHORT).show()
    }
}