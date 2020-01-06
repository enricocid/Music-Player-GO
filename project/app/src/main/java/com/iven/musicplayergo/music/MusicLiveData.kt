package com.iven.musicplayergo.music

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData

class MusicLiveData<T> : MutableLiveData<T>() {

    /**
     * re-implemented post method, as the original implementation may swallow date changes
     * by ignored all data before last [postValue] call
     */
    override fun postValue(value: T) {
        if (Thread.currentThread() == Looper.getMainLooper().thread)
            setValue(value)
        else
            Handler(Looper.getMainLooper()).post { setValue(value) }
    }
}
