package com.iven.musicplayergo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.musicLibrary

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val musicLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            performLoadMusic()
            it.value = true
        }
    }

    private fun performLoadMusic() {
        musicLibrary.queryForMusic(getApplication())
    }

    fun getMusic(): LiveData<Boolean> {
        return musicLiveData
    }
}
