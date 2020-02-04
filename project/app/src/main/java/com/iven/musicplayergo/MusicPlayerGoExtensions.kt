package com.iven.musicplayergo

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.iven.musicplayergo.ui.ThemeHelper
import kotlin.math.max
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

fun FragmentTransaction.addFragment(isAdd: Boolean, container: Int, fragment: Fragment) {
    when {
        isAdd -> {
            addToBackStack(null)
            add(
                container,
                fragment
            )
        }
        else -> replace(
            container,
            fragment
        )
    }
    commit()
}

fun View.createCircularReveal(isCentered: Boolean, show: Boolean): Animator {

    val revealDuration: Long = if (isCentered) 1500 else 500
    val radius = max(width, height).toFloat()

    val startRadius = if (show) 0f else radius
    val finalRadius = if (show) radius else 0f

    val cx = if (isCentered) width / 2 else 0
    val cy = if (isCentered) height / 2 else 0
    val animator =
        ViewAnimationUtils.createCircularReveal(
            this,
            cx,
            cy,
            startRadius,
            finalRadius
        ).apply {
            interpolator = FastOutSlowInInterpolator()
            duration = revealDuration
            start()
        }

    val accent = if (isCentered) ContextCompat.getColor(
        context,
        R.color.red
    ) else ThemeHelper.resolveThemeAccent(context)
    val backgroundColor =
        ThemeHelper.resolveColorAttr(context, android.R.attr.windowBackground)
    val startColor = if (show) accent else backgroundColor
    val endColor = if (show) backgroundColor else accent

    ValueAnimator().apply {
        setIntValues(startColor, endColor)
        setEvaluator(ArgbEvaluator())
        addUpdateListener { valueAnimator -> setBackgroundColor((valueAnimator.animatedValue as Int)) }
        duration = revealDuration
        if (isCentered) doOnEnd {
            background = ThemeHelper.createColouredRipple(
                context,
                ContextCompat.getColor(context, R.color.red),
                R.drawable.ripple
            )
        }
        start()
    }
    return animator
}

fun RecyclerView.smoothSnapToPosition(position: Int) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun getHorizontalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun onStop() {
            super.onStop()
            findViewHolderForAdapterPosition(position)
                ?.itemView?.performClick()
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

fun View.handleViewVisibility(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun String.toToast(
    context: Context
) {
    Toast.makeText(context, this, Toast.LENGTH_LONG).show()
}
