package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.handleViewVisibility
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper

class ActiveTabsAdapter(
        private val context: Context
) :
        RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    private val mAvailableItems = goPreferences.prefsActiveFragmentsDefault
    private val mActiveItems = goPreferences.activeFragments.toMutableList()

    fun getUpdatedItems(): Set<Int> {
        // make sure to respect tabs order
        mActiveItems.sortBy { it }
        return mActiveItems.toSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        return CheckableItemsHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.active_tab_item,
                        parent,
                        false
                )
        )
    }

    override fun getItemCount() = mAvailableItems.size

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems() {

            itemView.apply {

                val tabImageButton = findViewById<ImageButton>(R.id.tab_image)
                val indicator = findViewById<ImageButton>(R.id.tab_indicator)

                tabImageButton.setImageResource(ThemeHelper.getTabIcon(adapterPosition))

                isEnabled = adapterPosition != mAvailableItems.size - 1

                if (isEnabled) {
                    manageIndicatorsStatus(
                            mActiveItems.contains(adapterPosition),
                            tabImageButton,
                            indicator
                    )
                } else {
                    indicator.apply {
                        visibility = View.VISIBLE
                        drawable.alpha = ThemeHelper.getAlphaForAccent()
                    }
                    ThemeHelper.updateIconTint(
                            tabImageButton,
                            ThemeHelper.getAlphaAccent(context, ThemeHelper.getAlphaForAccent())
                    )
                }

                setOnClickListener {

                    manageIndicatorsStatus(
                            indicator.visibility != View.VISIBLE,
                            tabImageButton,
                            indicator
                    )

                    if (indicator.visibility != View.VISIBLE) mActiveItems.remove(
                            adapterPosition
                    ) else mActiveItems.add(adapterPosition)
                    if (mActiveItems.size < 2) {
                        context.getString(R.string.active_fragments_pref_warning)
                                .toToast(context)
                        mActiveItems.add(adapterPosition)
                        manageIndicatorsStatus(true, tabImageButton, indicator)
                    }
                }
            }
        }
    }

    private fun manageIndicatorsStatus(
            condition: Boolean,
            icon: ImageButton,
            indicator: ImageView
    ) {
        when {
            condition -> {
                indicator.handleViewVisibility(true)
                ThemeHelper.updateIconTint(icon, ThemeHelper.resolveThemeAccent(context))
            }
            else -> {
                indicator.handleViewVisibility(false)
                ThemeHelper.updateIconTint(
                        icon,
                        ThemeHelper.getAlphaAccent(context, ThemeHelper.getAlphaForAccent())
                )
            }
        }
    }
}
