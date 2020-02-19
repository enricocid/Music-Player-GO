package com.iven.musicplayergo.loader

import android.content.Context
import com.iven.musicplayergo.musicLibrary

class MusicLoader(
    context: Context
) : WrappedAsyncTaskLoader<Any?>(context) {
    // This is where background code is executed
    override fun loadInBackground() = musicLibrary.queryForMusic(context)
}

