package com.iven.musicplayergo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.musicLibrary

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val musicLiveData: MutableLiveData<MutableList<Music>> = MutableLiveData()

    private fun performLoadMusic() {
        val music = musicLibrary.queryForMusic(getApplication())
        musicLibrary.buildLibrary(getApplication(), music)
        musicLiveData.value = music
    }

    fun loadMusic(): LiveData<MutableList<Music>> {
        performLoadMusic()
        return musicLiveData
    }
}
