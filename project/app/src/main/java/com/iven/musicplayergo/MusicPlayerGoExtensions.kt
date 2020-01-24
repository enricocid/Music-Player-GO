package com.iven.musicplayergo

import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import com.iven.musicplayergo.ui.ThemeHelper
import kotlin.random.Random

//viewTreeObserver extension to measure layout params
//https://antonioleiva.com/kotlin-ongloballayoutlistener/
inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

//extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    val hexColor = Integer.toHexString(color).substring(2)
    val html = "<font color='#$hexColor'>$title</font>"
    title = ThemeHelper.buildSpanned(html)
}

fun IntRange.random() = Random.nextInt(start, endInclusive + 1)
