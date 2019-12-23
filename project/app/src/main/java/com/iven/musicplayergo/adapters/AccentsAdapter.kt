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

    override fun getItemCount(): Int {
        return mAccents.size
    }

    override fun onBindViewHolder(holder: AccentsHolder, position: Int) {
        holder.bindItems(mAccents[holder.adapterPosition].first)
    }

    inner class AccentsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {

            itemView.apply {

                ThemeHelper.getColor(
                    context,
                    color,
                    R.color.deep_purple
                ).apply { ThemeHelper.updateIconTint(findViewById(R.id.circle), this) }

                findViewById<ImageView>(R.id.check).apply {
                    visibility = if (color != mSelectedAccent)
                        View.GONE
                    else
                        View.VISIBLE
                }

                setOnClickListener {

                    if (mAccents[adapterPosition].first != mSelectedAccent) {

                        mSelectedAccent = mAccents[adapterPosition].first
                        goPreferences.accent = mSelectedAccent

                        ThemeHelper.applyNewThemeSmoothly(
                            activity
                        )
                    }
                }
            }
        }
    }
}
