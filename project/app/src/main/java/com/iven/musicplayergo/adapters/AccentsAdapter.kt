package com.iven.musicplayergo.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.handleViewVisibility
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper

class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private val mAccents = ThemeHelper.accents
    private var mSelectedAccent = goPreferences.accent

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentsHolder {
        return AccentsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.accent_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = mAccents.size

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(mAccents[holder.adapterPosition].first)
    }

    inner class AccentsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {

            itemView.run {

                val circle = findViewById<ImageButton>(R.id.circle)
                val accent = ContextCompat.getColor(activity, color)
                ThemeHelper.updateIconTint(circle, accent)
                ThemeHelper.createColouredRipple(activity, accent, R.drawable.ripple_oval).apply {
                    itemView.background = this
                }

                contentDescription = ThemeHelper.getAccentName(mAccents[adapterPosition].first, activity)
                findViewById<ImageButton>(R.id.check).handleViewVisibility(color == mSelectedAccent)

                setOnClickListener {
                    if (mAccents[adapterPosition].first != mSelectedAccent) {
                        mSelectedAccent = mAccents[adapterPosition].first
                        goPreferences.accent = mSelectedAccent
                    }
                }
            }
        }
    }
}
