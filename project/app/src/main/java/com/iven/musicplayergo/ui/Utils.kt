package com.iven.musicplayergo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.afollestad.recyclical.datasource.DataSource
import com.pranavpandey.android.dynamic.toasts.DynamicToast


object Utils {

    @JvmStatic
    fun makeUnknownErrorToast(context: Context, message: Int) {
        DynamicToast.makeError(context, context.getString(message), Toast.LENGTH_LONG)
            .show()
    }

    @JvmStatic
    fun setupSearchViewForStringLists(
        searchView: SearchView,
        list: List<String>,
        dataSource: DataSource<Any>
    ) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override
            fun onQueryTextChange(newText: String): Boolean {
                processQueryForStringsLists(newText, list, dataSource)
                return false
            }

            override
            fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
    }

    @JvmStatic
    @SuppressLint("DefaultLocale")
    private fun processQueryForStringsLists(
        query: String,
        list: List<String>,
        dataSource: DataSource<Any>
    ) {
        // in real app you'd have it instantiated just once
        val results = mutableListOf<Any>()

        try {
            // case insensitive search
            list.iterator().forEach {
                if (it.toLowerCase().startsWith(query.toLowerCase())) {
                    results.add(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.size > 0) {
            dataSource.set(results)
        }
    }
}
