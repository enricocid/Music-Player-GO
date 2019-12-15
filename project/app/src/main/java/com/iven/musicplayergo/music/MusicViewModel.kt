package com.iven.musicplayergo.music

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iven.musicplayergo.musicLibrary
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MusicViewModel : ViewModel(), CoroutineScope {

    private val hasLoaded: MutableLiveData<Boolean> = MutableLiveData()

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main + handler

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    fun loadMusic(context: Context): MutableLiveData<Boolean> {
        launch {
            try {
                withContext(Dispatchers.Main) {
                    musicLibrary.build(context)
                    hasLoaded.value = true
                }
            } catch (e: Exception) {
                hasLoaded.value = false
                e.printStackTrace()
            }
        }
        return hasLoaded
    }
}
