package com.iven.musicplayergo.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.goPreferences

class ThemesAdapter(private val activity: Activity) :
    RecyclerView.Adapter<ThemesAdapter.ThemesHolder>() {

    private val mThemesList = activity.resources.getStringArray(R.array.themeListArray)
    private val mThemesValues = activity.resources.getStringArray(R.array.themeEntryArray)
    private var mSelectedTheme = activity.getString(R.string.theme_pref_light)

    init {
        mSelectedTheme = goPreferences.theme!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemesHolder {
        return ThemesHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.theming_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mThemesValues.size
    }

    override fun onBindViewHolder(holder: ThemesHolder, position: Int) {
        holder.bindItems(mThemesList[holder.adapterPosition])
    }

    inner class ThemesHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(theme: String) {

            val title = itemView.findViewById<TextView>(R.id.title)
            title.text = theme
            title.isSelected = true

            val radioButton = itemView.findViewById<AppCompatRadioButton>(R.id.radiobutton)
            radioButton.isChecked = mThemesValues[adapterPosition] == mSelectedTheme

            itemView.setOnClickListener {

                if (mThemesValues[adapterPosition] != mSelectedTheme) {

                    mSelectedTheme = mThemesValues[adapterPosition]
                    goPreferences.theme = mSelectedTheme

                    ThemeHelper.applyNewThemeSmoothly(activity)
                }
            }
        }
    }
}
