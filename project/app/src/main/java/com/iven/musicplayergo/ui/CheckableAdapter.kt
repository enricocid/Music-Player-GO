package com.iven.musicplayergo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import java.util.*

class CheckableAdapter(
    private val listItems: MutableList<String>
) :
    RecyclerView.Adapter<CheckableAdapter.HiddenItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mCheckableItems = listItems


    init {
        Collections.sort(listItems, String.CASE_INSENSITIVE_ORDER)
    }

    fun getUpdatedItems(): Set<String> {
        mCheckableItems.removeAll(mItemsToRemove.toSet())
        return mCheckableItems.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiddenItemsHolder {
        return HiddenItemsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.checkable_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: HiddenItemsHolder, position: Int) {
        holder.bindItems(listItems[holder.adapterPosition])
    }

    inner class HiddenItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(item: String) {

            val title = itemView.findViewById<TextView>(R.id.title)
            title.text = item
            title.isSelected = true

            val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)
            /*if (!isHiddenItemsDialog) {
                itemView.isEnabled = adapterPosition != listItems.size - 1
                checkBox.isEnabled = itemView.isEnabled
                checkBox.isChecked = mCheckableItems.contains(adapterPosition.toString())
            } else {*/
                checkBox.isChecked = listItems.contains(item)
            /*}*/

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
                 if (!checkBox.isChecked) {
                        mItemsToRemove.add(item)
                    } else {
                        if (mItemsToRemove.contains(item)) mItemsToRemove.remove(item)
                    }
            }
        }
    }
}
