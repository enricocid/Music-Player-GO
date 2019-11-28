package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.Utils

class CheckableAdapter(
    private val context: Context,
    private val listItems: MutableList<String>
) :
    RecyclerView.Adapter<CheckableAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mCheckableItems = goPreferences.activeFragments!!.toMutableList()

    fun getUpdatedItems(): Set<String> {
        mCheckableItems.removeAll(mItemsToRemove.toSet())
        return mCheckableItems.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        return CheckableItemsHolder(
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

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems(listItems[holder.adapterPosition])
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(item: String) {

            val title = itemView.findViewById<TextView>(R.id.title)
            title.text = item
            title.isSelected = true

            val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)

            itemView.isEnabled = adapterPosition != listItems.size - 1
            checkBox.isEnabled = itemView.isEnabled
            checkBox.isChecked = mCheckableItems.contains(adapterPosition.toString())

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked

                if (!checkBox.isChecked) mCheckableItems.remove(adapterPosition.toString()) else mCheckableItems.add(
                    adapterPosition.toString()
                )
                if (mCheckableItems.size < listItems.size - 2) {
                    Utils.makeToast(
                        context,
                        context.getString(R.string.active_fragments_pref_warning)
                    )
                    mCheckableItems.add(adapterPosition.toString())
                    checkBox.isChecked = true
                }
            }
        }
    }
}
