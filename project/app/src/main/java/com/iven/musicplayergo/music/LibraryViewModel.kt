package com.iven.musicplayergo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.musicLibrary

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    val buildLibraryLiveData: MutableLiveData<Boolean> = MutableLiveData()

    private fun performBuildMusicLibrary() {
        musicLibrary.buildLibrary(getApplication(), musicLibrary.allSongsUnfiltered)
        buildLibraryLiveData.value = true
    }

    fun buildMusicLibrary(): LiveData<Boolean> {
        performBuildMusicLibrary()
        return buildLibraryLiveData
    }
}
