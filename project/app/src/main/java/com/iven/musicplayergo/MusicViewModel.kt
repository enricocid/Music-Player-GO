package com.iven.musicplayergo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.iven.musicplayergo.models.Music
import kotlinx.coroutines.*

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * This is the job for all coroutines started by this ViewModel.
     * Cancelling this job will cancel all coroutines started by this ViewModel.
     */
    private val viewModelJob = SupervisorJob()

    private val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        deviceMusic.value = null
    }

    private val uiDispatcher = Dispatchers.Main
    private val ioDispatcher = Dispatchers.IO + viewModelJob + handler
    private val uiScope = CoroutineScope(uiDispatcher)

    val deviceMusic = MutableLiveData<MutableList<Music>?>()

    /**
     * Cancel all coroutines when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getDeviceMusic() {

        val musicRepository = MusicRepository.getInstance()

        uiScope.launch {
            withContext(ioDispatcher) {
                val music =
                    musicRepository.getDeviceMusic(getApplication()) // get default_cover from MediaStore on IO thread
                withContext(uiDispatcher) {
                    deviceMusic.value = music // post values on Main thread
                }
            }
        }
    }
}
