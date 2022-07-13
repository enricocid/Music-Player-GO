package com.iven.musicplayergo.preferences

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.handleViewVisibility
import com.iven.musicplayergo.extensions.toContrastColor
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.extensions.updateIconTint
import com.iven.musicplayergo.utils.Theming


class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private val mAccents = activity.resources.getIntArray(R.array.colors)
    var selectedAccent = GoPreferences.getPrefsInstance().accent

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AccentsHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.accent_item,
            parent,
            false
        )
    )

    override fun getItemCount() = mAccents.size

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(mAccents[holder.absoluteAdapterPosition])
    }

    inner class AccentsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {

            with(itemView) {

                val accentFullName = Theming.getAccentName(resources, absoluteAdapterPosition)
                contentDescription = accentFullName

                val cardView = this as MaterialCardView
                cardView.setCardBackgroundColor(color)

                findViewById<ImageView>(R.id.check).run {
                    handleViewVisibility(
                        show = absoluteAdapterPosition == selectedAccent
                    )
                    updateIconTint(ColorUtils.setAlphaComponent(color.toContrastColor(), 75))
                }

                setOnClickListener {
                    if (absoluteAdapterPosition != selectedAccent) {
                        notifyItemChanged(selectedAccent)
                        selectedAccent = absoluteAdapterPosition
                        notifyItemChanged(absoluteAdapterPosition)
                    }
                }

                setOnLongClickListener {
                    accentFullName.toToast(activity)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
