package com.iven.musicplayergo.musicloadutils

import android.content.Context
import androidx.loader.content.AsyncTaskLoader

/**
 * [Issue
 * 14944](http://code.google.com/p/android/issues/detail?id=14944)
 *
 * @author Alexander Blom
 */

/**
 * Constructor of `WrappedAsyncTaskLoader`
 *
 * @param context The [Context] to use.
 */
abstract class WrappedAsyncTaskLoader<D>(context: Context) : AsyncTaskLoader<D>(context) {

    private var mData: D? = null
    /**
     * {@inheritDoc}
     */
    override fun deliverResult(data: D?) {
        if (!isReset) {
            mData = data
            super.deliverResult(data)
        } // else ... An asynchronous query came in while the loader is stopped
    }

    /**
     * {@inheritDoc}
     */
    override fun onStartLoading() {
        super.onStartLoading()
        if (mData != null) {
            deliverResult(mData)
        } else if (takeContentChanged() || mData == null) {
            forceLoad()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onStopLoading() {
        super.onStopLoading()
        // Attempt to cancel the current load task if possible
        cancelLoad()
    }

    /**
     * {@inheritDoc}
     */
    override fun onReset() {
        super.onReset()
        // Ensure the loader is stopped
        onStopLoading()
        mData = null
    }
}
