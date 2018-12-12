package com.iven.musicplayergo.uihelpers

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.iven.musicplayergo.adapters.ArtistsAdapter

object UIUtils {

    fun getColor(context: Context, color: Int, emergencyColor: Int): Int {
        return try {
            ContextCompat.getColor(context, color)
        } catch (e: Exception) {
            ContextCompat.getColor(context, emergencyColor)
        }
    }

    fun setupSearch(
        searchView: SearchView,
        artistsAdapter: ArtistsAdapter,
        artists: List<String>,
        waveView: WaveView
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
            waveView.visibility = if (hasFocus) View.GONE else View.VISIBLE
            if (!hasFocus) searchView.isIconified = true
        }
    }

    private fun processQuery(query: String, artistsAdapter: ArtistsAdapter, artists: List<String>) {
        // in real app you'd have it instantiated just once
        val results = mutableListOf<String>()

        try {
            // case insensitive search
            artists.forEach {
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

    fun setHorizontalScrollBehavior(parentView: View, vararg textViews: TextView) {
        var isLongPressed = false

        parentView.setOnLongClickListener {
            if (!isLongPressed) {
                textViews.forEachIndexed { _, textView ->
                    textView.isSelected = true
                }
                isLongPressed = true
            }
            return@setOnLongClickListener true
        }

        parentView.setOnTouchListener { _, e ->
            if (isLongPressed && e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_OUTSIDE || e.action == MotionEvent.ACTION_MOVE) {

                textViews.forEach {
                    it.isSelected = false
                }
                isLongPressed = false
            }
            return@setOnTouchListener false
        }
    }
}