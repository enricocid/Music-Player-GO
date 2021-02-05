package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import java.util.*

class ActiveTabsAdapter(private val ctx: Context) :
    RecyclerView.Adapter<ActiveTabsAdapter.CheckableItemsHolder>() {

    private val mAvailableItems = goPreferences.activeTabsDef.toMutableList()
    private val mActiveItems = goPreferences.activeTabs.toMutableList()

    //method used to make the last item of the staggered rv full width
    //https://medium.com/android-dev-journal/how-to-make-first-item-of-recyclerview-of-full-width-with-a-gridlayoutmanager-66456a4bfffe
    val spanSizeLookup: GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == mAvailableItems.size - 1) {
                    2
                } else {
                    1
                }
            }
        }

    fun getUpdatedItems(): List<String> {
        goPreferences.activeTabsDef = mAvailableItems
        // make sure to respect tabs order
        val differences = mAvailableItems.minus(mActiveItems)
        return mAvailableItems.minus(differences)
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

            itemView.run {

                val tabImageButton = findViewById<ImageButton>(R.id.tab_image)

                tabImageButton.setImageResource(ThemeHelper.getTabIcon(mAvailableItems[adapterPosition]))

                isEnabled = mAvailableItems[adapterPosition] != GoConstants.SETTINGS_TAB
                isClickable = isEnabled

                if (isEnabled) {
                    manageTabStatus(
                        mActiveItems.contains(mAvailableItems[adapterPosition]),
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

                    val toggledItem = mAvailableItems[adapterPosition]
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

    // https://mobikul.com/drag-and-drop-item-on-recyclerview/
    val itemTouchCallback: ItemTouchHelper.Callback = object : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags =
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return (mAvailableItems[viewHolder.adapterPosition] != GoConstants.SETTINGS_TAB).apply {
                if (this) {
                    onItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                }
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    private fun onItemMoved(fromPosition: Int, toPosition: Int) {
        if (mAvailableItems[toPosition] != GoConstants.SETTINGS_TAB) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(mAvailableItems, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(mAvailableItems, i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }
    }
}
