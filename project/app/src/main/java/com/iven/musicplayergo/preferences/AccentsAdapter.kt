package com.iven.musicplayergo.preferences

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
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

                (this as MaterialCardView).run {
                    setCardBackgroundColor(color)
                    radius = resources.getDimensionPixelSize(
                        if (absoluteAdapterPosition != selectedAccent) {
                            strokeWidth = 0
                            strokeColor = Color.TRANSPARENT
                            R.dimen.accent_dim_radius
                        } else {
                            strokeColor = ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.widgetsColor), 100)
                            strokeWidth = resources.getDimensionPixelSize(R.dimen.search_bar_elevation)
                            R.dimen.accent_dim_radius_uns
                        }
                    ).toFloat()
                }

                setOnClickListener {
                    if (absoluteAdapterPosition != selectedAccent) {
                        notifyItemChanged(selectedAccent)
                        selectedAccent = absoluteAdapterPosition
                        notifyItemChanged(absoluteAdapterPosition)
                    }
                }

                setOnLongClickListener {
                    Toast.makeText(activity, accentFullName, Toast.LENGTH_SHORT)
                        .show()
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
