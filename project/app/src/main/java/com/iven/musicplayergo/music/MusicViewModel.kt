package com.iven.musicplayergo.music

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.musicLibrary

class MusicViewModel : ViewModel() {

    private val hasLoaded: MutableLiveData<Boolean> = MutableLiveData()

    fun loadMusic(context: Context): MutableLiveData<Boolean> {
        hasLoaded.value = musicLibrary.build(context)
        return hasLoaded
    }
}
