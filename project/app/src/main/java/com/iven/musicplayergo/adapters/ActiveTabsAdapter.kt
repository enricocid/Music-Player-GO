package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper

class ActiveTabsAdapter(
    private val context: Context
) :
    RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    private val mAvailableItems = goPreferences.prefsActiveFragmentsDef
    private val mActiveItems = goPreferences.activeFragments.toMutableList()

    // Colors
    private val mResolvedAccentColor by lazy { ThemeHelper.resolveThemeAccent(context) }
    private val mResolvedAlphaAccentColor by lazy {
        ThemeHelper.getAlphaAccent(
            context,
            ThemeHelper.getAlphaForAccent()
        )
    }

    //method used to make the last item of the staggered rv full width
    //https://medium.com/android-dev-journal/how-to-make-first-item-of-recyclerview-of-full-width-with-a-gridlayoutmanager-66456a4bfffe
    val spanSizeLookup: GridLayoutManager.SpanSizeLookup by lazy {
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == mAvailableItems.size - 1) {
                    2
                } else {
                    1
                }
            }
        }
    }

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

                tabImageButton.setImageResource(ThemeHelper.getTabIcon(adapterPosition))

                val cardView = itemView as MaterialCardView

                isEnabled = adapterPosition != mAvailableItems.size - 1

                if (isEnabled) {
                    manageIndicatorsStatus(
                        mActiveItems.contains(adapterPosition),
                        tabImageButton,
                        cardView
                    )
                } else {
                    cardView.apply {
                        cardView.strokeWidth = 4
                        cardView.strokeColor = mResolvedAlphaAccentColor
                    }
                    ThemeHelper.updateIconTint(
                        tabImageButton,
                        mResolvedAlphaAccentColor
                    )
                }

                setOnClickListener {

                    manageIndicatorsStatus(
                        cardView.strokeWidth != 4,
                        tabImageButton,
                        cardView
                    )

                    if (cardView.strokeWidth != 4) {
                        mActiveItems.remove(
                            adapterPosition
                        )
                    } else {
                        mActiveItems.add(adapterPosition)
                    }
                    if (mActiveItems.size < 2) {
                        context.getString(R.string.active_fragments_pref_warning)
                            .toToast(context)
                        mActiveItems.add(adapterPosition)
                        manageIndicatorsStatus(true, tabImageButton, cardView)
                    }
                }
            }
        }
    }

    private fun manageIndicatorsStatus(
        condition: Boolean,
        icon: ImageButton,
        cardView: MaterialCardView
    ) {
        when {
            condition -> {
                cardView.strokeWidth = 4
                ThemeHelper.updateIconTint(icon, mResolvedAccentColor)
            }
            else -> {
                cardView.strokeWidth = 0
                ThemeHelper.updateIconTint(
                    icon,
                    mResolvedAlphaAccentColor
                )
            }
        }
    }
}
