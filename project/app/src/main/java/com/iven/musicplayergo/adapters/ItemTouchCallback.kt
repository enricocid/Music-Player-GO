package com.iven.musicplayergo.adapters

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ItemTouchCallback<T>(private val collection: MutableList<T>) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags =
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
    ) = onItemMoved(viewHolder.adapterPosition, target.adapterPosition, recyclerView.adapter)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled() = true

    private fun onItemMoved(fromPosition: Int, toPosition: Int, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?) : Boolean {
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
        return true
    }
}
