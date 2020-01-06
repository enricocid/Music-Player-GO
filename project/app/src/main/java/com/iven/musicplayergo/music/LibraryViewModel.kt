package com.iven.musicplayergo.music

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.musicLibrary
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LibraryViewModel : ViewModel(), CoroutineScope {

    private val libraryLiveData: MusicLiveData<HashMap<String, List<Album>>> by lazy {
        MusicLiveData<HashMap<String, List<Album>>>()
    }

    private val buildLibraryJob = Job()

    override val coroutineContext: CoroutineContext
        get() = buildLibraryJob + Dispatchers.Main + handler

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    // Extension method to get all music files list from external storage/sd card
    @SuppressLint("InlinedApi")
    fun getMutableLiveData(
        context: Context,
        deviceSongs: MutableList<Music>?
    ): MutableLiveData<HashMap<String, List<Album>>> {

        if (libraryLiveData.value.isNullOrEmpty()) {
            launch {
                try {
                    withContext(Dispatchers.Main) {
                        val library = musicLibrary.buildLibrary(context, deviceSongs)
                        libraryLiveData.postValue(library)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return libraryLiveData
    }
}
