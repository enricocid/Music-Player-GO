package com.iven.musicplayergo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.models.Music
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class MusicViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    val deviceMusic = MutableLiveData<MutableList<Music>?>()

    fun getDeviceMusic() {

        val musicRepository = MusicRepository.getInstance()

        launch {
            val music =
                musicRepository.getDeviceMusic(getApplication()) // get music from MediaStore on IO thread
            withContext(coroutineContext) {
                deviceMusic.value = music // post values on Main thread
            }
        }
    }
}
