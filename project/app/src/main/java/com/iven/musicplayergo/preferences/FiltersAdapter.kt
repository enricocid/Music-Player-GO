package com.iven.musicplayergo.preferences

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.utils.Theming


class FiltersAdapter(val activity: Activity) :
    RecyclerView.Adapter<FiltersAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mAvailableItems = goPreferences.filters?.sorted()?.toMutableList()

    private val mDisabledColor = Theming.resolveColorAttr(
        activity,
        android.R.attr.colorButtonNormal
    )

    private val mDefaultTextColor = Theming.resolveColorAttr(activity, android.R.attr.textColorPrimary)

    fun getUpdatedItems() = mAvailableItems?.apply {
        removeAll(mItemsToRemove.toSet())
    }?.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CheckableItemsHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.filter_item,
            parent,
            false
        )
    )

    override fun getItemCount() = mAvailableItems?.size!!

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems(mAvailableItems?.get(position))
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(itemFilter: String?) {

            with(itemView as MaterialCheckBox) {
                text = itemFilter
                setOnCheckedChangeListener { _, b ->
                    if (b) {
                        setTextColor(mDefaultTextColor)
                        mItemsToRemove.remove(itemFilter)
                    } else {
                        setTextColor(mDisabledColor)
                        mItemsToRemove.add(itemFilter!!)
                    }
                }
            }
        }
    }
}
