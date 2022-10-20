package com.iven.musicplayergo.preferences

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.databinding.FilterItemBinding
import com.iven.musicplayergo.utils.Theming


class FiltersAdapter(val activity: Activity) :
    RecyclerView.Adapter<FiltersAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private val mAvailableItems = GoPreferences.getPrefsInstance().filters?.sorted()?.toMutableList()

    private val mDisabledColor = Theming.resolveWidgetsColorNormal(activity)

    private val mDefaultTextColor = Theming.resolveColorAttr(activity, android.R.attr.textColorPrimary)

    fun getUpdatedItems() = mAvailableItems?.apply {
        removeAll(mItemsToRemove.toSet())
    }?.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        val binding = FilterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CheckableItemsHolder(binding)
    }

    override fun getItemCount() = mAvailableItems?.size!!

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems(mAvailableItems?.get(position))
    }

    inner class CheckableItemsHolder(private val binding: FilterItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindItems(itemFilter: String?) {

            with(binding.root) {
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
