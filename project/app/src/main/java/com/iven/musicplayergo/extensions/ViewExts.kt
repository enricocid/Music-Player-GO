package com.iven.musicplayergo.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.InsetDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.window.layout.WindowMetricsCalculator
import coil.load
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.SingleClickHelper
import com.iven.musicplayergo.utils.Theming
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

fun View.applyEdgeToEdge() {
    setOnApplyWindowInsetsListener { view, insets ->
        val bars = WindowInsetsCompat.toWindowInsetsCompat(insets)
            .getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(left = bars.left, right = bars.right)
        insets
    }
}

fun ImageView.loadWithError(bitmap: Bitmap?, error: Boolean, albumArt: Int) {
    if (error) {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        load(ContextCompat.getDrawable(context, albumArt)?.toBitmap())
        return
    }
    scaleType = ImageView.ScaleType.CENTER_CROP
    load(bitmap)
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

// Extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
}

// Extension to set menu items icon color
fun MenuItem.setIconTint(color: Int) {
    icon?.let { dw ->
        val wrapped = DrawableCompat.wrap(dw)
        DrawableCompat.setTint(wrapped, color)
        icon = wrapped
    }
}

// Extension to set span to menu title
fun MenuItem.setTitle(resources: Resources, title: String?) {
    SpannableString(title).apply {
        setSpan(RelativeSizeSpan(0.75f), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(
            ForegroundColorSpan(Theming.resolveThemeColor(resources)),
            0,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setTitle(this)
    }
}

@SuppressLint("RestrictedApi")
fun Menu.enablePopupIcons(activity: Activity) {
    val iconMarginPx = activity.resources.getDimensionPixelSize(R.dimen.player_controls_padding_start)

    if (this is MenuBuilder) {
        setOptionalIconsVisible(true)
        val visibleItemsIterator = visibleItems.iterator()
        while (visibleItemsIterator.hasNext()) {
            with(visibleItemsIterator.next()) {
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
            add(R.id.container, fm, tag)
        }
    }
}

fun FragmentManager.goBackFromFragmentNow(fragment: Fragment?) {
    if (backStackEntryCount >= 0) {
        commit {
            fragment?.run {
                remove(this)
            }
            popBackStackImmediate()
        }
    }
}

fun FragmentManager.isFragment(fragmentTag: String): Boolean {
    val df = findFragmentByTag(fragmentTag)
    return df != null && df.isVisible && df.isAdded
}

fun View.createCircularReveal(show: Boolean): Animator {

    val revealDuration = 500L
    val radius = max(width, height).toFloat()
    var startRadius = radius
    var finalRadius = 0F
    if (show) {
        startRadius = 0F
        finalRadius = radius
    }

    val animator = ViewAnimationUtils.createCircularReveal(this, 0, 0,
        startRadius, finalRadius).apply {
            interpolator = FastOutSlowInInterpolator()
            duration = revealDuration
            doOnEnd {
                if (!show) {
                    handleViewVisibility(show = false)
                }
            }
            start()
        }

    val mainBackground = Theming.resolveColorAttr(context, R.attr.main_bg)

    with(ValueAnimator()) {
        setIntValues(mainBackground, mainBackground)
        setEvaluator(ArgbEvaluatorCompat())
        addUpdateListener { valueAnimator -> setBackgroundColor((valueAnimator.animatedValue as Int)) }
        duration = revealDuration
        start()
    }
    return animator
}

// https://stackoverflow.com/a/53986874
fun RecyclerView.smoothSnapToPosition(position: Int) {
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun getHorizontalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun onStop() {
            super.onStop()
            findViewHolderForAdapterPosition(position)?.itemView?.performClick()
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

fun View.handleViewVisibility(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun View.safeClickListener(safeClickListener: (view: View) -> Unit) {
    setOnClickListener {
        if (!SingleClickHelper.isBlockingClick()) {
            safeClickListener(it)
        }
    }
}

fun ImageView.updateIconTint(tint: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tint))
}


fun Dialog?.applyFullHeightDialog(activity: Activity) {
    // to ensure full dialog's height

    val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val height = windowMetrics.bounds.height()

    this?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bs ->
        BottomSheetBehavior.from(bs).peekHeight = height
    }
}
