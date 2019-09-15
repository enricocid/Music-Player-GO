package com.iven.musicplayergo

import android.content.Context
import android.widget.Toast
import com.pranavpandey.android.dynamic.toasts.DynamicToast

object Utils {

    @JvmStatic
    fun makeUnknownErrorToast(context: Context, message: Int) {
        DynamicToast.makeError(context, context.getString(message), Toast.LENGTH_LONG)
            .show()
    }
}
