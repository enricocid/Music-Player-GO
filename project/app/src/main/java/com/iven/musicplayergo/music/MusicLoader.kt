package com.iven.musicplayergo.music

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.iven.musicplayergo.musicLibrary

class MusicLoader(
    context: Context
) : AsyncTaskLoader<Boolean>(context) {

    override fun onStartLoading() {
        super.onStartLoading()
        forceLoad()
    }

    override fun loadInBackground(): Boolean {
        /*This is where background code is executed*/
        return musicLibrary.queryForMusic(context)
    }
}
