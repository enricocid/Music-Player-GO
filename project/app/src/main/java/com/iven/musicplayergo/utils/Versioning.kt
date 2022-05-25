package com.iven.musicplayergo.utils

import android.os.Build

object Versioning {

    @JvmStatic
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @JvmStatic
    fun isOreoMR1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    @JvmStatic
    fun isMarshmallow() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}
