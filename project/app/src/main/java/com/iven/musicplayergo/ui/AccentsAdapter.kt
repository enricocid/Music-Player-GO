package com.iven.musicplayergo.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences

class AccentsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<AccentsAdapter.AccentsHolder>() {

    private var mSelectedAccent = R.color.deep_purple

    init {
        mSelectedAccent = goPreferences.accent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentsHolder {
        return AccentsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.theming_item,
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

            val title = itemView.findViewById<TextView>(R.id.title)
            title.text = getResourceName(color)
            title.isSelected = true

            val radioButton = itemView.findViewById<AppCompatRadioButton>(R.id.radiobutton)

            val accent = ThemeHelper.getColor(activity, color, R.color.deep_purple)
            radioButton.isChecked = color == mSelectedAccent

            val widgetColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled), //enabled
                    intArrayOf(android.R.attr.state_enabled) //disabled
                ),
                intArrayOf(accent, accent)
            )

            title.setTextColor(widgetColor)
            radioButton.buttonTintList = widgetColor

            itemView.setOnClickListener {

                if (ThemeHelper.accents[adapterPosition].first != mSelectedAccent) {

                    mSelectedAccent = ThemeHelper.accents[adapterPosition].first
                    goPreferences.accent = mSelectedAccent

                    ThemeHelper.applyNewThemeSmoothly(activity)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getResourceName(res: Int): String {
        return try {
            activity.resources.getResourceEntryName(res)
                .replace(
                    activity.getString(R.string.underscore_delimiter),
                    activity.getString(R.string.space_delimiter)
                ).capitalize()
        } catch (e: Exception) {
            e.printStackTrace()
            Utils.makeToast(activity, R.string.error_get_resource)
            ""
        }
    }
}
