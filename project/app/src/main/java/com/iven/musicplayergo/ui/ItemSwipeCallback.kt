package com.iven.musicplayergo.ui

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.utils.Theming


class ItemSwipeCallback(private val isQueueDialog: Boolean, private val isFavoritesDialog: Boolean, private val onSwipedAction: (viewHolder: RecyclerView.ViewHolder, direction: Int) -> Unit): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) {
        onSwipedAction(viewHolder, direction)
    }

    // Credits to Aidan Follestad :)
    // https://github.com/afollestad/recyclical/blob/master/swipe/src/main/java/com/afollestad/recyclical/swipe/SwipeItemTouchListener.kt#L120
    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val context = recyclerView.context
        val colorDrawableBackground = ColorDrawable(Theming.resolveWidgetsColorNormal(context))
        val icon: Drawable?
        var iconMarginVertical: Int

        val itemView = viewHolder.itemView

        if (dX > 0) {

            icon = ContextCompat.getDrawable(context, if (isQueueDialog && !isFavoritesDialog) {
                R.drawable.ic_delete
            } else {
                R.drawable.ic_queue_add
            })

            icon?.run {
                iconMarginVertical = (viewHolder.itemView.height - intrinsicHeight) / 2
                colorDrawableBackground.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                setBounds(
                    itemView.left + iconMarginVertical,
                    itemView.top + iconMarginVertical,
                    itemView.left + iconMarginVertical + intrinsicWidth,
                    itemView.bottom - iconMarginVertical
                )
            }

        } else {

            colorDrawableBackground.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )

            icon = ContextCompat.getDrawable(context, if (isQueueDialog || isFavoritesDialog) {
                R.drawable.ic_delete
            } else {
                R.drawable.ic_favorite
            })

            icon?.run {
                iconMarginVertical = (viewHolder.itemView.height - intrinsicHeight) / 2
                setBounds(
                    itemView.right - iconMarginVertical - intrinsicWidth,
                    itemView.top + iconMarginVertical,
                    itemView.right - iconMarginVertical,
                    itemView.bottom - iconMarginVertical
                )
                level = 0
            }
        }

        with(c) {
            colorDrawableBackground.draw(this)
            save()
            if (dX > 0) {
                clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
            } else {
                clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            }
            icon?.draw(this)
            restore()
            super.onChildDraw(this, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}
