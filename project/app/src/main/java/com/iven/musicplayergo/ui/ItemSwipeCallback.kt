package com.iven.musicplayergo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R


class ItemSwipeCallback(private val ctx: Context, private val isDialog: Boolean, private val onSwipedAction: (viewHolder: RecyclerView.ViewHolder, direction: Int) -> Unit) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

    private val red = ContextCompat.getColor(ctx, R.color.red)

    private val firstIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_queue_add).apply {
        this?.mutate()?.setTint(ContextCompat.getColor(ctx, R.color.green))
    }

    private val firstIconAlt = ContextCompat.getDrawable(ctx, R.drawable.ic_delete).apply {
        this?.mutate()?.setTint(red)
    }

    private val secondIcon = ContextCompat.getDrawable(
            ctx,
            if (isDialog) {
                R.drawable.ic_delete
            } else {
                R.drawable.ic_favorite
            }
    ).apply {
        this?.mutate()?.setTint(red)
    }

    private val firstColor = ColorDrawable(ContextCompat.getColor(ctx, R.color.swipeActionDeleteColor))

    private val secondColor = if (isDialog) {
        firstColor
    } else {
        ColorDrawable(ContextCompat.getColor(ctx, R.color.swipeActionAddColor))
    }

    private var background = secondColor

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

        val itemView = viewHolder.itemView

        when {
            dX > 0 -> {
                if (isDialog) {
                    firstIconAlt
                } else {
                    background = secondColor
                    firstIcon
                }?.let { icon ->
                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = iconLeft + icon.intrinsicWidth

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                    )
                    background.draw(c)
                    icon.draw(c)
                }
            }
            dX < 0 -> {
                background = firstColor

                secondIcon?.let { icon ->

                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - icon.intrinsicWidth

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                    )
                    background.draw(c)
                    icon.draw(c)
                }
            }
            else -> {
                background.setBounds(0, 0, 0, 0)
            }
        }
    }
}
