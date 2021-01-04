package com.iven.musicplayergo.helpers

import android.os.Build

object VersioningHelper {

    @JvmStatic
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @JvmStatic
    fun isOreoMR1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
}
