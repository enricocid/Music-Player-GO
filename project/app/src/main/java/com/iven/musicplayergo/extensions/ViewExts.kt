package com.iven.musicplayergo.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.iven.musicplayergo.R
import com.iven.musicplayergo.fragments.DetailsFragment.Companion.DETAILS_FRAGMENT_TAG
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import kotlin.math.max

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

@ColorInt
fun Int.decodeColor(context: Context) = ContextCompat.getColor(context, this)

@Suppress("DEPRECATION")
fun String.toSpanned(): Spanned = if (VersioningHelper.isNougat()) Html.fromHtml(
    this,
    Html.FROM_HTML_MODE_LEGACY
)
else Html.fromHtml(this)

//extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
}

fun FragmentManager.addFragment(fragment: Fragment, tag: String?) {
    beginTransaction().apply {
        addToBackStack(null)
        add(
            R.id.container,
            fragment,
            tag
        )
        commit()
    }
}

fun FragmentManager.isDetailsFragment(): Boolean {
    val df = findFragmentByTag(DETAILS_FRAGMENT_TAG)
    return df != null && df.isVisible && df.isAdded
}

fun View.createCircularReveal(isErrorFragment: Boolean, show: Boolean): Animator {

    val revealDuration: Long = if (isErrorFragment) 1500 else 500
    val radius = max(width, height).toFloat()

    val startRadius = if (show) 0f else radius
    val finalRadius = if (show) radius else 0f

    val cx = if (isErrorFragment) width / 2 else 0
    val cy = if (isErrorFragment) height / 2 else 0
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
                if (!show) visibility = View.GONE
            }
            start()
        }

    val windowBackground = R.color.windowBackground.decodeColor(context)
    val red = R.color.red.decodeColor(context)
    val accent = if (!show) windowBackground else ThemeHelper.resolveThemeAccent(context)

    val startColor = if (isErrorFragment) red else accent
    val endColor = if (show) windowBackground else red

    ValueAnimator().apply {
        setIntValues(startColor, endColor)
        setEvaluator(ArgbEvaluatorCompat())
        addUpdateListener { valueAnimator -> setBackgroundColor((valueAnimator.animatedValue as Int)) }
        duration = revealDuration
        if (isErrorFragment) doOnEnd {
            background =
                ThemeHelper.createColouredRipple(
                    context,
                    R.color.red.decodeColor(context),
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
