package com.iven.musicplayergo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.R

class ColorsAdapter(private val context: Context, private val accent: Int) :
    RecyclerView.Adapter<ColorsAdapter.ColorsHolder>() {

    var onColorClick: ((Int) -> Unit)? = null

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorsHolder {
        return ColorsHolder(LayoutInflater.from(parent.context).inflate(R.layout.color_option, parent, false))
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
            val drawable = if (color != accent) R.drawable.ic_checkbox_blank else R.drawable.ic_checkbox_marked
            val colorFromInt = ContextCompat.getColor(context, color)
            colorOption.setImageResource(drawable)
            colorOption.setColorFilter(colorFromInt)
            itemView.setOnClickListener { onColorClick?.invoke(color) }
        }
    }
}