package com.iven.musicplayergo.preferences

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.AccentItemBinding
import com.iven.musicplayergo.utils.Theming


class AccentsAdapter(private val accents: IntArray):
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    var selectedAccent = GoPreferences.getPrefsInstance().accent

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentsHolder {
        val binding = AccentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccentsHolder(binding)
    }

    override fun getItemCount() = accents.size

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(accents[holder.absoluteAdapterPosition])
    }

    inner class AccentsHolder(private val binding: AccentItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bindItems(color: Int) {

            with(binding.root) {

                val accentFullName = Theming.getAccentName(resources, absoluteAdapterPosition)
                contentDescription = accentFullName

                setCardBackgroundColor(color)
                radius = resources.getDimensionPixelSize(
                    if (absoluteAdapterPosition != selectedAccent) {
                        strokeWidth = 0
                        R.dimen.accent_radius
                    } else {
                        strokeWidth = resources.getDimensionPixelSize(R.dimen.accent_stroke)
                        val stroke = if (ColorUtils.calculateLuminance(color) < 0.35) Color.WHITE else Color.DKGRAY
                        strokeColor = ColorUtils.setAlphaComponent(stroke, 75)
                        R.dimen.accent_radius_alt
                    }
                ).toFloat()

                setOnClickListener {
                    if (absoluteAdapterPosition != selectedAccent) {
                        notifyItemChanged(selectedAccent)
                        selectedAccent = absoluteAdapterPosition
                        notifyItemChanged(absoluteAdapterPosition)
                    }
                }

                setOnLongClickListener {
                    Toast.makeText(context, accentFullName, Toast.LENGTH_SHORT)
                        .show()
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
