package com.iven.musicplayergo.music

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.musicLibrary

class MusicViewModel : ViewModel() {

    val musicLiveData: MutableLiveData<MutableList<Music>> = MutableLiveData()

    private fun loadAllDeviceMusic(context: Context) {
        val music = musicLibrary.queryForMusic(context)
        musicLiveData.value = music
    }

    fun getMusicLiveData(context: Context): LiveData<MutableList<Music>> {
        loadAllDeviceMusic(context)
        return musicLiveData
    }
}
