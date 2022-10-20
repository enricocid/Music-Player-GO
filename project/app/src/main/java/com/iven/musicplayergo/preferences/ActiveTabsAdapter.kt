package com.iven.musicplayergo.preferences

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.ActiveTabItemBinding
import com.iven.musicplayergo.extensions.updateIconTint
import com.iven.musicplayergo.utils.Theming


class ActiveTabsAdapter(private val ctx: Context) :
    RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    var availableItems = GoPreferences.getPrefsInstance().activeTabsDef.toMutableList()
    private val mActiveItems = GoPreferences.getPrefsInstance().activeTabs.toMutableList()

    private val mDisabledColor = Theming.resolveWidgetsColorNormal(ctx)
    private val mDefaultTextColor = Theming.resolveColorAttr(ctx, android.R.attr.textColorPrimary)

    fun getUpdatedItems() = availableItems.apply {
        GoPreferences.getPrefsInstance().activeTabsDef = this
    }.minus(availableItems.minus(mActiveItems.toSet()).toSet()) /*make sure to respect tabs order*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableItemsHolder {
        val binding = ActiveTabItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CheckableItemsHolder(binding)
    }

    override fun getItemCount() = availableItems.size

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(private val binding: ActiveTabItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindItems() {

            with(binding) {

                tabText.text = ctx.getString(getTabText(availableItems[absoluteAdapterPosition]))

                tabImage.setImageResource(Theming.getTabIcon(availableItems[absoluteAdapterPosition]))

                root.isEnabled = availableItems[absoluteAdapterPosition] != GoConstants.SETTINGS_TAB
                root.isClickable = root.isEnabled

                if (root.isEnabled) {
                    manageTabStatus(
                        selected = mActiveItems.contains(availableItems[absoluteAdapterPosition]),
                        tabDragHandle,
                        tabText,
                        tabImage
                    )
                } else {
                    tabDragHandle.updateIconTint(mDisabledColor)
                    tabText.setTextColor(mDisabledColor)
                    tabImage.updateIconTint(mDisabledColor)
                }

                root.setOnClickListener {

                    manageTabStatus(
                        selected = !tabImage.isSelected,
                        tabDragHandle,
                        tabText,
                        tabImage
                    )

                    val toggledItem = availableItems[absoluteAdapterPosition]
                    if (!tabImage.isSelected) {
                        mActiveItems.remove(toggledItem)
                    } else {
                        mActiveItems.add(toggledItem)
                    }
                    if (mActiveItems.size < 2) {
                        mActiveItems.add(toggledItem)
                        manageTabStatus(selected = true, tabDragHandle, tabText, tabImage)
                    }
                }
            }
        }
    }

    private fun manageTabStatus(
        selected: Boolean,
        dragHandle: ImageView,
        textView: TextView,
        icon: ImageView
    ) {
        icon.isSelected = selected
        val iconColor = if (selected) {
            Theming.resolveThemeColor(icon.resources)
        } else {
            mDisabledColor
        }
        val textColor = if (selected) {
            mDefaultTextColor
        } else {
            mDisabledColor
        }
        dragHandle.updateIconTint(textColor)
        textView.setTextColor(textColor)
        icon.updateIconTint(iconColor)
    }

    private fun getTabText(tab: String) = when (tab) {
        GoConstants.ARTISTS_TAB -> R.string.artists
        GoConstants.ALBUM_TAB -> R.string.albums
        GoConstants.SONGS_TAB -> R.string.songs
        GoConstants.FOLDERS_TAB -> R.string.folders
        else -> R.string.settings
    }
}
