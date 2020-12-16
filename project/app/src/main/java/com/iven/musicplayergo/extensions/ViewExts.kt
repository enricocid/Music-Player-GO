package com.iven.musicplayergo.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transform.RoundedCornersTransformation
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.iven.musicplayergo.R
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Music
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import kotlinx.coroutines.*
import kotlin.math.max

// viewTreeObserver extension to measure layout params
// https://antonioleiva.com/kotlin-ongloballayoutlistener/
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

/**
 * This is the job for all coroutines started by this ViewModel.
 * Cancelling this job will cancel all coroutines started by this ViewModel.
 */
private val viewModelJob = SupervisorJob()

private val handler = CoroutineExceptionHandler { _, exception ->
    exception.printStackTrace()
}

private val ioDispatcher = Dispatchers.IO + viewModelJob + handler
private val ioScope = CoroutineScope(ioDispatcher)

fun Context.getImageLoader() = ImageLoader.Builder(this)
    .bitmapPoolingEnabled(false)
    .crossfade(true)
    .build()

fun ImageView.loadCover(
    imageLoader: ImageLoader,
    music: Music?,
    defaultCover: Bitmap?,
    isCircleCrop: Boolean,
    isLoadDelay: Boolean
) {

    val request = ImageRequest.Builder(context)
        .data(music?.getCover(context) ?: defaultCover)
        .scale(Scale.FIT)
        .size(ViewSizeResolver(this))
        .target(
            onSuccess = { result ->
                // Handle the successful result.
                load(result) {
                    if (isCircleCrop) {
                        transformations(RoundedCornersTransformation(resources.getDimension(R.dimen.md_corner_radius)))
                    }
                }
            }
        )
        .build()

    ioScope.launch {
        withContext(ioDispatcher) {
            delay(
                if (isLoadDelay) {
                    1000
                } else {
                    0
                }
            )
            imageLoader.enqueue(request)
        }
    }
}

// Extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
}

fun FragmentManager.addFragment(fragment: Fragment, tag: String?) {
    commit {
        addToBackStack(null)
        add(
            R.id.container,
            fragment,
            tag
        )
    }
}

fun FragmentManager.goBackFromFragment(isFragmentExpanded: Boolean) {
    if (isFragmentExpanded && backStackEntryCount >= 1) {
        popBackStack()
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

    ValueAnimator().apply {
        setIntValues(startColor, endColor)
        setEvaluator(ArgbEvaluatorCompat())
        addUpdateListener { valueAnimator -> setBackgroundColor((valueAnimator.animatedValue as Int)) }
        duration = revealDuration
        if (isErrorFragment) {
            doOnEnd {
                background =
                    ThemeHelper.createColouredRipple(
                        context,
                        ContextCompat.getColor(context, R.color.red),
                        R.drawable.ripple
                    )
            }
        }
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

//add swipe features to a RecyclerView
fun RecyclerView.addBidirectionalSwipeHandler(
    isRightToLeftEnabled: Boolean,
    onSwiped: (
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) -> Unit
) {
    val swipeLeftCallback = instantiateSwipeHandler(ItemTouchHelper.RIGHT, onSwiped)
    val swipeLeftHelper = ItemTouchHelper(swipeLeftCallback)
    swipeLeftHelper.attachToRecyclerView(this)
    if (isRightToLeftEnabled) {
        val swipeRightCallback = instantiateSwipeHandler(ItemTouchHelper.LEFT, onSwiped)
        val swipeRightHelper = ItemTouchHelper(swipeRightCallback)
        swipeRightHelper.attachToRecyclerView(this)
    }
}

private fun instantiateSwipeHandler(
    direction: Int,
    onSwiped: (
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) -> Unit
): ItemTouchHelper.SimpleCallback {
    return object : ItemTouchHelper.SimpleCallback(
        0,
        direction
    ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            onSwiped(viewHolder, direction)
        }
    }
}

fun View.handleViewVisibility(show: Boolean) {
    visibility = if (show) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

fun String.toToast(
    context: Context
) {
    Toast.makeText(context, this, Toast.LENGTH_LONG).show()
}
