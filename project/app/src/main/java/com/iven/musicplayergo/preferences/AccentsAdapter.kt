package com.iven.musicplayergo.preferences

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toContrastColor
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper

class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private val mAccents = ThemeHelper.accents
    var selectedAccent = goPreferences.accent

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AccentsHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.accent_item,
            parent,
            false
        )
    )

    override fun getItemCount() = mAccents.size

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(mAccents[holder.absoluteAdapterPosition].first)
    }

    inner class AccentsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {

            itemView.run {

                val accent = ContextCompat.getColor(activity, color)
                val accentFullName = ThemeHelper.getAccentName(activity, mAccents[absoluteAdapterPosition].first)
                contentDescription = accentFullName

                val cardView = this as MaterialCardView
                cardView.setCardBackgroundColor(accent)
                cardView.strokeColor = ColorUtils.setAlphaComponent(accent.toContrastColor(), 90)

                cardView.strokeWidth = if (color == selectedAccent) {
                    resources.getDimensionPixelSize(R.dimen.accent_dim_stroke)
                } else {
                    0
                }

                setOnClickListener {
                    if (mAccents[absoluteAdapterPosition].first != selectedAccent) {
                        notifyItemChanged(mAccents.indexOfFirst {
                            it.first == selectedAccent
                        })
                        selectedAccent = mAccents[absoluteAdapterPosition].first
                        notifyItemChanged(absoluteAdapterPosition)
                    }
                }

                setOnLongClickListener {
                    accentFullName.toString().toToast(activity)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
