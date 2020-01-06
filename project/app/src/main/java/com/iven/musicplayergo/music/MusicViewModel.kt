package com.iven.musicplayergo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.musicLibrary
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MusicViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private var music: MutableList<Music>? = null

    val musicLiveData: MutableLiveData<MutableList<Music>> by lazy {
        MutableLiveData<MutableList<Music>>()
    }

    private val viewModelJob = GlobalScope.launch {

        music = musicLibrary.queryForMusic(application.applicationContext)
        musicLibrary.buildLibrary(application.applicationContext, music)

        withContext(Dispatchers.Main) {
            musicLiveData.value = music
        }
    }

    override val coroutineContext: CoroutineContext
        get() = viewModelJob + Dispatchers.Main + handler

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getMusicLiveData(): LiveData<MutableList<Music>> {
        launch { viewModelJob }
        return musicLiveData
    }
}
