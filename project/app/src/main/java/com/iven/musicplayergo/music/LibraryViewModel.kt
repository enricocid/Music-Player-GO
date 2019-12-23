package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.musicLibrary
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LibraryViewModel : ViewModel(), CoroutineScope {

    private val libraryLiveData: MutableLiveData<HashMap<String, List<Album>>> = MutableLiveData()

    private val buildLibraryJob = Job()

    override val coroutineContext: CoroutineContext
        get() = buildLibraryJob + Dispatchers.Main + handler

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    fun buildLibrary(
        context: Context,
        deviceSongs: MutableList<Music>?
    ): MutableLiveData<HashMap<String, List<Album>>> {
        return go(context, deviceSongs)
    }

    // Extension method to get all music files list from external storage/sd card
    @SuppressLint("InlinedApi")
    fun go(
        context: Context,
        deviceSongs: MutableList<Music>?
    ): MutableLiveData<HashMap<String, List<Album>>> {

        launch {
            try {
                withContext(Dispatchers.Main) {
                    libraryLiveData.value = musicLibrary.buildLibrary(context, deviceSongs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return libraryLiveData
    }
}
