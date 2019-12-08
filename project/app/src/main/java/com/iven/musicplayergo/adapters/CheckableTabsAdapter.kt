package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.Utils

class CheckableTabsAdapter(
    private val context: Context,
    private val listItems: MutableList<String>
) :
    RecyclerView.Adapter<CheckableTabsAdapter.CheckableItemsHolder>() {

    private val mItemsToRemove = mutableListOf<String>()

    private var mCheckableItems = goPreferences.activeFragments?.toMutableList()

    fun getUpdatedItems(): Set<String>? {
        mCheckableItems?.removeAll(mItemsToRemove.toSet())
        return mCheckableItems?.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        return CheckableItemsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.checkable_tab_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems() {

            itemView.apply {

                val icon = findViewById<ImageView>(R.id.tab_image)
                icon.setImageResource(ThemeHelper.getTabIcon(adapterPosition))
                val indicator = findViewById<ImageView>(R.id.tab_indicator)

                isEnabled = adapterPosition != listItems.size - 1
                if (isEnabled) {
                    manageIndicatorsStatus(
                        mCheckableItems?.contains(adapterPosition.toString())!!,
                        icon,
                        indicator
                    )
                } else {
                    indicator.apply {
                        visibility = View.VISIBLE
                        drawable.alpha = 50
                    }
                    ThemeHelper.updateIconTint(icon, ThemeHelper.getAlphaAccent(context, 50))
                }

                setOnClickListener {

                    manageIndicatorsStatus(indicator.visibility != View.VISIBLE, icon, indicator)

                    if (indicator.visibility != View.VISIBLE) mCheckableItems?.remove(
                        adapterPosition.toString()
                    ) else mCheckableItems?.add(
                        adapterPosition.toString()
                    )
                    if (mCheckableItems?.size!! < listItems.size - 2) {
                        Utils.makeToast(
                            context,
                            context.getString(R.string.active_fragments_pref_warning)
                        )
                        mCheckableItems?.add(adapterPosition.toString())
                        manageIndicatorsStatus(true, icon, indicator)
                    }
                }
            }
        }
    }

    private fun manageIndicatorsStatus(condition: Boolean, icon: ImageView, indicator: ImageView) {
        when {
            condition -> {
                indicator.visibility = View.VISIBLE
                ThemeHelper.updateIconTint(icon, ThemeHelper.resolveThemeAccent(context))
            }
            else -> {
                indicator.visibility = View.GONE
                ThemeHelper.updateIconTint(icon, ThemeHelper.getAlphaAccent(context, 50))
            }
        }
    }
}
