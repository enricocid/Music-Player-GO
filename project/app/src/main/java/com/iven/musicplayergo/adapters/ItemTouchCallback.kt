package com.iven.musicplayergo.adapters

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class ItemTouchCallback<T>(private val collection: MutableList<T>, private val isActiveTabs: Boolean) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ) : Boolean {
        return (isActiveTabs && viewHolder.adapterPosition != collection.size -1 || !isActiveTabs).apply {
            if (this) {
                onItemMove(viewHolder.adapterPosition, target.adapterPosition, recyclerView.adapter)
            }
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled() = true

    private fun onItemMove(fromPosition: Int, toPosition: Int, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?) : Boolean {

        if (isActiveTabs && toPosition != collection.size -1 || !isActiveTabs) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(collection, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(collection, i, i - 1)
                }
            }
            adapter?.notifyItemMoved(fromPosition, toPosition)
        }
        return true
    }
}
