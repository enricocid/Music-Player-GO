package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper


class ActiveTabsAdapter(private val ctx: Context) :
    RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    var availableItems = goPreferences.activeTabsDef.toMutableList()
    private val mActiveItems = goPreferences.activeTabs.toMutableList()

    //method used to make the last item of the staggered rv full width
    //https://medium.com/android-dev-journal/how-to-make-first-item-of-recyclerview-of-full-width-with-a-gridlayoutmanager-66456a4bfffe
    val spanSizeLookup: GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == availableItems.size - 1) {
                    2
                } else {
                    1
                }
            }
        }

    fun getUpdatedItems(): List<String> {
        goPreferences.activeTabsDef = availableItems
        // make sure to respect tabs order
        val differences = availableItems.minus(mActiveItems)
        return availableItems.minus(differences)
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

    override fun getItemCount() = availableItems.size

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems() {

            itemView.run {

                val tabImageButton = findViewById<ImageButton>(R.id.tab_image)

                tabImageButton.setImageResource(ThemeHelper.getTabIcon(availableItems[adapterPosition]))

                isEnabled = availableItems[adapterPosition] != GoConstants.SETTINGS_TAB
                isClickable = isEnabled

                if (isEnabled) {
                    manageTabStatus(
                        mActiveItems.contains(availableItems[adapterPosition]),
                        tabImageButton
                    )
                } else {
                    ThemeHelper.updateIconTint(
                        tabImageButton,
                        ThemeHelper.getAlphaAccent(ctx)
                    )
                }

                setOnClickListener {
                    manageTabStatus(
                        !tabImageButton.isSelected,
                        tabImageButton
                    )

                    val toggledItem = availableItems[adapterPosition]
                    if (!tabImageButton.isSelected) {
                        mActiveItems.remove(toggledItem)
                    } else {
                        mActiveItems.add(toggledItem)
                    }
                    if (mActiveItems.size < 2) {
                        Toast.makeText(context,  context.getString(R.string.active_fragments_pref_warning), Toast.LENGTH_LONG)
                                .show()
                        mActiveItems.add(toggledItem)
                        manageTabStatus(true, tabImageButton)
                    }
                }
            }
        }
    }

    private fun manageTabStatus(
        condition: Boolean,
        icon: ImageButton
    ) {
        icon.isSelected = condition
        val color = if (condition) {
            ThemeHelper.resolveThemeAccent(ctx)
        } else {
            ThemeHelper.getAlphaAccent(ctx)
        }
        ThemeHelper.updateIconTint(icon, color)
    }
}
