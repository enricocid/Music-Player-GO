package com.iven.musicplayergo.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

/**
 * <a href="http://code.google.com/p/android/issues/detail?id=14944">Issue
 * 14944</a>
 *
 * @author Alexander Blom
 */
abstract class WrappedAsyncTaskLoader<D> extends AsyncTaskLoader<D> {

    private D mData;

    /**
     * Constructor of <code>WrappedAsyncTaskLoader</code>
     *
     * @param context The {@link Context} to use.
     */
    WrappedAsyncTaskLoader(@NonNull final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliverResult(D data) {
        if (!isReset()) {
            this.mData = data;
            super.deliverResult(data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        if (this.mData != null) {
            deliverResult(this.mData);
        } else if (takeContentChanged() || this.mData == null) {
            forceLoad();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        // Attempt to cancel the current load task if possible
        cancelLoad();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onReset() {
        super.onReset();
        // Ensure the loader is stopped
        onStopLoading();
        this.mData = null;
    }
}
