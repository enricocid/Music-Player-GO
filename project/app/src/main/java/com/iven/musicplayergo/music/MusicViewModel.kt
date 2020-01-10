package com.iven.musicplayergo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.musicLibrary

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val musicLiveData: MutableLiveData<MutableList<Music>> = MutableLiveData()

    private fun loadAllDeviceMusic() {
        val music = musicLibrary.queryForMusic(getApplication())
        musicLiveData.value = music
    }

    fun getMusicLiveData(): LiveData<MutableList<Music>> {
        loadAllDeviceMusic()
        return musicLiveData
    }
}
