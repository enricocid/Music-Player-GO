package com.iven.musicplayergo.adapters

import android.app.Activity
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.Utils
import com.iven.musicplayergo.mMusicPlayerGoPreferences

class ColorsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<ColorsAdapter.ColorsHolder>() {

    //fixed int array of accent colors
    private val mColors = intArrayOf(
        R.color.red,
        R.color.pink,
        R.color.purple,
        R.color.deep_purple,
        R.color.indigo,
        R.color.blue,
        R.color.light_blue,
        R.color.cyan,
        R.color.teal,
        R.color.green,
        R.color.amber,
        R.color.orange,
        R.color.deep_orange,
        R.color.brown,
        R.color.gray,
        R.color.blue_gray
    )

    private var mSelectedColor = R.color.blue

    init {
        mSelectedColor = mMusicPlayerGoPreferences.accent
    }

    private fun getColorPosition(color: Int): Int {
        return try {
            mColors.indexOf(color)
        } catch (e: Exception) {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorsHolder {
        return ColorsHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.color_option,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mColors.size
    }

    override fun onBindViewHolder(holder: ColorsHolder, position: Int) {
        holder.bindItems(mColors[holder.adapterPosition])
    }

    inner class ColorsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(color: Int) {
            val colorOption = itemView as ImageView
            val drawable = if (color != mSelectedColor) R.drawable.ic_checkbox_blank
            else
                R.drawable.ic_checkbox_marked

            val colorFromInt = Utils.getColor(activity, color, R.color.blue)
            colorOption.setImageResource(drawable)
            colorOption.setColorFilter(colorFromInt)
            itemView.setOnClickListener {

                if (mColors[adapterPosition] != mSelectedColor) {
                    notifyItemChanged(getColorPosition(mSelectedColor))
                    mSelectedColor = mColors[adapterPosition]
                    colorOption.setImageResource(R.drawable.ic_checkbox_marked)
                    mMusicPlayerGoPreferences.accent = mSelectedColor

                    Handler().postDelayed({
                        Utils.applyNewThemeSmoothly(activity)
                    }, 250)
                }
            }
        }
    }
}
