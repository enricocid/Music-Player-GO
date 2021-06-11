package com.iven.musicplayergo.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper


class FiltersAdapter(val activity: Activity) :
    RecyclerView.Adapter<FiltersAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mAvailableItems = goPreferences.filters?.sorted()?.toMutableList()

    private val mDisabledColor = ThemeHelper.resolveColorAttr(
        activity,
        android.R.attr.colorButtonNormal
    )

    private val mDefaultTextColor = ThemeHelper.resolveColorAttr(activity, android.R.attr.textColorPrimary)

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

                setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                    if (checkBox.isChecked) {
                        filter.setTextColor(mDefaultTextColor)
                        mItemsToRemove.remove(item)
                    } else {
                        filter.setTextColor(mDisabledColor)
                        mItemsToRemove.add(item!!)
                    }
                }
            }
        }
    }
}
