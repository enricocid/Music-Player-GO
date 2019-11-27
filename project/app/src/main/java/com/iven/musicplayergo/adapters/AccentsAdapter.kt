package com.iven.musicplayergo.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper

class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private var mSelectedAccent = R.color.deep_purple

    init {
        mSelectedAccent = goPreferences.accent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentsHolder {
        return AccentsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.accent_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return ThemeHelper.accents.size
    }

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(ThemeHelper.accents[holder.adapterPosition].first)
    }

    inner class AccentsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {

            val circle = itemView.findViewById<ImageView>(R.id.circle)

            val accent = ThemeHelper.getColor(
                activity,
                color,
                R.color.deep_purple
            )
            ThemeHelper.updateIconTint(circle, accent)

            val check = itemView.findViewById<ImageView>(R.id.check)
            check.visibility = if (color != mSelectedAccent) View.GONE else View.VISIBLE

            itemView.setOnClickListener {

                if (ThemeHelper.accents[adapterPosition].first != mSelectedAccent) {

                    mSelectedAccent = ThemeHelper.accents[adapterPosition].first
                    goPreferences.accent = mSelectedAccent

                    ThemeHelper.applyNewThemeSmoothly(
                        activity
                    )
                }
            }
        }
    }
}
