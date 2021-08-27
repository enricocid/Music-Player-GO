package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper


class ActiveTabsAdapter(private val ctx: Context) :
    RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    var availableItems = goPreferences.activeTabsDef.toMutableList()
    private val mActiveItems = goPreferences.activeTabs.toMutableList()

    private val mDisabledColor = ThemeHelper.resolveColorAttr(ctx, android.R.attr.colorButtonNormal)
    private val mDefaultTextColor = ThemeHelper.resolveColorAttr(ctx, android.R.attr.textColorPrimary)

    fun getUpdatedItems() = availableItems.apply {
        goPreferences.activeTabsDef = this
    }.minus(availableItems.minus(mActiveItems) /*make sure to respect tabs order*/)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CheckableItemsHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.active_tab_item,
            parent,
            false
        )
    )

    override fun getItemCount() = availableItems.size

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems() {

            itemView.run {

                val tabDragHandle = findViewById<ImageView>(R.id.tab_drag_handle)

                val tabText = findViewById<TextView>(R.id.tab_text)
                tabText.text = ctx.getString(getTabText(availableItems[absoluteAdapterPosition]))

                val tabImageButton = findViewById<ImageView>(R.id.tab_image)
                tabImageButton.setImageResource(ThemeHelper.getTabIcon(availableItems[absoluteAdapterPosition]))

                isEnabled = availableItems[absoluteAdapterPosition] != GoConstants.SETTINGS_TAB
                isClickable = isEnabled

                if (isEnabled) {
                    manageTabStatus(
                        selected = mActiveItems.contains(availableItems[absoluteAdapterPosition]),
                        tabDragHandle,
                        tabText,
                        tabImageButton
                    )
                } else {
                    ThemeHelper.updateIconTint(tabDragHandle, mDisabledColor)
                    tabText.setTextColor(mDisabledColor)
                    ThemeHelper.updateIconTint(
                        tabImageButton,
                        mDisabledColor
                    )
                }

                setOnClickListener {

                    manageTabStatus(
                        selected = !tabImageButton.isSelected,
                        tabDragHandle,
                        tabText,
                        tabImageButton
                    )

                    val toggledItem = availableItems[absoluteAdapterPosition]
                    if (!tabImageButton.isSelected) {
                        mActiveItems.remove(toggledItem)
                    } else {
                        mActiveItems.add(toggledItem)
                    }
                    if (mActiveItems.size < 2) {
                        Toast.makeText(context, R.string.active_fragments_pref_warning, Toast.LENGTH_LONG)
                            .show()
                        mActiveItems.add(toggledItem)
                        manageTabStatus(selected = true, tabDragHandle, tabText, tabImageButton)
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
            ThemeHelper.resolveThemeAccent(ctx)
        } else {
            mDisabledColor
        }
        val textColor = if (selected) {
            mDefaultTextColor
        } else {
            mDisabledColor
        }
        ThemeHelper.updateIconTint(dragHandle, textColor)
        textView.setTextColor(textColor)
        ThemeHelper.updateIconTint(icon, iconColor)
    }

    private fun getTabText(tab: String) = when (tab) {
        GoConstants.ARTISTS_TAB -> R.string.artists
        GoConstants.ALBUM_TAB -> R.string.albums
        GoConstants.SONGS_TAB -> R.string.songs
        GoConstants.FOLDERS_TAB -> R.string.folders
        else -> R.string.settings
    }
}
