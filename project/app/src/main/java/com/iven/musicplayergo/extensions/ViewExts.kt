package com.iven.musicplayergo.extensions

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.LoadRequest
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.Music

fun ImageView.loadCover(imageLoader: ImageLoader, music: Music?) {
    val request = LoadRequest.Builder(context)
        .data(music?.getCover(context))
        .target(this)
        .build()
    imageLoader.execute(request)
}

fun Window.applyEdgeToEdgeBottomSheet(resources: Resources) {
    val view = findViewById<View>(com.google.android.material.R.id.container)
    view?.fitsSystemWindows = false
    if (goPreferences.isEdgeToEdge && !ThemeHelper.isDeviceLand(resources)) {
        ThemeHelper.handleLightSystemBars(resources.configuration, decorView, true)
    }
}

fun Window.handleTransparentSystemBars() {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
}

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

@ColorInt
fun Int.decodeColor(context: Context) = ContextCompat.getColor(context, this)

@Suppress("DEPRECATION")
fun String.toSpanned(): Spanned = if (VersioningHelper.isNougat()) Html.fromHtml(
    this,
    Html.FROM_HTML_MODE_LEGACY
)
else Html.fromHtml(this)

// Extension to set menu items text color
fun MenuItem.setTitleColor(color: Int) {
    SpannableString(title).apply {
        setSpan(ForegroundColorSpan(color), 0, length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        title = this
    }
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
    visibility = if (show) View.VISIBLE else View.GONE
}

fun String.toToast(
    context: Context
) {
    Toast.makeText(context, this, Toast.LENGTH_LONG).show()
}
