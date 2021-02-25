package com.iven.musicplayergo.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.iven.musicplayergo.R
import com.iven.musicplayergo.helpers.ThemeHelper
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import kotlin.math.max


// viewTreeObserver extension to measure layout params
// https://antonioleiva.com/kotlin-ongloballayoutlistener/
inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

// https://stackoverflow.com/a/38241603
fun Toolbar.getTitleTextView() = try {
    val toolbarClass = Toolbar::class.java
    val titleTextViewField = toolbarClass.getDeclaredField("mTitleTextView")
    titleTextViewField.isAccessible = true
    titleTextViewField.get(this) as TextView
} catch (e: Exception) {
    e.printStackTrace()
    null
}

@SuppressLint("DefaultLocale")
fun String.getFastScrollerItem(context: Context): FastScrollItemIndicator {
    var charAtZero = context.getString(R.string.fastscroller_dummy_item)
    if (isNotEmpty()) {
        charAtZero = "${get(0)}"
    }
    return FastScrollItemIndicator.Text(
        charAtZero.toUpperCase() // Grab the first letter and capitalize it
    )
}

// Extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
}

// Extension to set span to menu title
fun MenuItem.setTitle(activity: Activity, title: String?) {

    val accent = ThemeHelper.resolveThemeAccent(activity)

    val spanString = SpannableString(if (title?.length!! > 20) {
        activity.getString(R.string.popup_menu_title, title.substring(0,20))
    } else {
        title
    })
    spanString.setSpan(RelativeSizeSpan(0.75f), 0, spanString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    spanString.setSpan(ForegroundColorSpan(accent), 0, spanString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    setTitle(spanString)
}

@SuppressLint("RestrictedApi")
fun Menu.enablePopupIcons(activity: Activity) {
    val iconMarginPx = activity.resources.getDimensionPixelSize(R.dimen.player_controls_padding_start)

    if (this is MenuBuilder) {
        setOptionalIconsVisible(true)
        val visibleItemsIterator = visibleItems.iterator()
        while (visibleItemsIterator.hasNext()) {
            visibleItemsIterator.next().run {
                icon?.let { ic ->
                    icon = InsetDrawable(ic, iconMarginPx, 0, iconMarginPx, 0)
                }
            }
        }
    }
}

fun FragmentManager.addFragment(fragment: Fragment?, tag: String?) {
    fragment?.let { fm ->
        commit {
            addToBackStack(null)
            add(
                    R.id.container,
                    fm,
                    tag
            )
        }
    }
}

fun FragmentManager.goBackFromFragmentNow(fragment: Fragment?) {
    if (backStackEntryCount >= 0) {
        commit {
            fragment?.let { fm ->
                remove(fm)
            }
            popBackStack()
        }
    }
}

fun FragmentManager.isFragment(fragmentTag: String): Boolean {
    val df = findFragmentByTag(fragmentTag)
    return df != null && df.isVisible && df.isAdded
}

fun View.createCircularReveal(isErrorFragment: Boolean, show: Boolean): Animator {

    val revealDuration: Long = if (isErrorFragment) {
        1500
    } else {
        500
    }
    val radius = max(width, height).toFloat()

    val startRadius = if (show) {
        0f
    } else {
        radius
    }
    val finalRadius = if (show) {
        radius
    } else {
        0f
    }

    val cx = if (isErrorFragment) {
        width / 2
    } else {
        0
    }
    val cy = if (isErrorFragment) {
        height / 2
    } else {
        0
    }
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
            doOnEnd {
                if (!show) {
                    handleViewVisibility(false)
                }
            }
            start()
        }

    val windowBackground = ContextCompat.getColor(context, R.color.windowBackground)
    val closeColor = ThemeHelper.resolveColorAttr(context, R.attr.colorControlHighlight)
    val accent = if (!show) {
        windowBackground
    } else {
        ThemeHelper.resolveThemeAccent(context)
    }

    val startColor = if (isErrorFragment) {
        ContextCompat.getColor(context, R.color.red)
    } else {
        accent
    }
    val endColor = if (show) {
        windowBackground
    } else {
        closeColor
    }

    with(ValueAnimator()) {
        setIntValues(startColor, endColor)
        setEvaluator(ArgbEvaluatorCompat())
        addUpdateListener { valueAnimator -> setBackgroundColor((valueAnimator.animatedValue as Int)) }
        duration = revealDuration
        start()
    }

    return animator
}

// https://stackoverflow.com/a/53986874
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

fun View.handleViewVisibility(show: Boolean) {
    visibility = if (show) {
        View.VISIBLE
    } else {
        View.GONE
    }
}
