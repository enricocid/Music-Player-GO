package com.iven.musicplayergo.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences


class FiltersAdapter :
    RecyclerView.Adapter<FiltersAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mAvailableItems = goPreferences.filters?.sorted()?.toMutableList()

    fun getUpdatedItems(): Set<String>? {
        mAvailableItems?.removeAll(mItemsToRemove.toSet())
        return mAvailableItems?.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        return CheckableItemsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.filter_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = mAvailableItems?.size!!

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems(mAvailableItems?.get(position))
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(item: String?) {

            itemView.run {

                val checkBox = findViewById<MaterialCheckBox>(R.id.checkbox)
                val filter = findViewById<TextView>(R.id.filter).apply {
                    text = item
                }
                val filterPaintFlags = filter.paintFlags

                setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked

                    if (checkBox.isChecked) {
                        filter.paintFlags = filterPaintFlags
                        mItemsToRemove.remove(item)
                    } else {
                        filter.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG or filterPaintFlags
                        mItemsToRemove.add(item!!)
                    }
                }
            }
        }
    }
}
