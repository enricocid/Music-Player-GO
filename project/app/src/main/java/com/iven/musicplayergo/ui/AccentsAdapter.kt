package com.iven.musicplayergo.ui

import android.app.Activity
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.musicPlayerGoExAppPreferences

class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private var mSelectedAccent = R.color.deepPurple

    init {
        mSelectedAccent = musicPlayerGoExAppPreferences.accent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentsHolder {
        return AccentsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.recycler_view_accent_item,
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

            val colorOption = itemView as ImageView
            val drawable = if (color != mSelectedAccent) 0
            else
                R.drawable.ic_check

            val colorFromInt = ThemeHelper.getColor(activity, color, R.color.deepPurple)
            itemView.setBackgroundColor(colorFromInt)

            colorOption.setImageResource(drawable)

            itemView.setOnClickListener {

                if (ThemeHelper.accents[adapterPosition].first != mSelectedAccent) {

                    notifyItemChanged(ThemeHelper.getAccent(mSelectedAccent).second)

                    mSelectedAccent = ThemeHelper.accents[adapterPosition].first
                    colorOption.setImageResource(R.drawable.ic_check)
                    musicPlayerGoExAppPreferences.accent = mSelectedAccent

                    Handler().postDelayed({
                        ThemeHelper.applyNewThemeSmoothly(activity)
                    }, 250)
                }
            }
        }
    }
}
