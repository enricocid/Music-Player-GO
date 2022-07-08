package com.iven.musicplayergo.preferences

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.models.NotificationAction
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.utils.Theming


class NotificationActionsAdapter(private val ctx: Context, private val mediaPlayerHolder: MediaPlayerHolder?) :
    RecyclerView.Adapter<NotificationActionsAdapter.CheckableItemsHolder>() {

    var selectedActions = GoPreferences.getPrefsInstance().notificationActions

    private val mActions = listOf(
        NotificationAction(GoConstants.REPEAT_ACTION, GoConstants.CLOSE_ACTION), // default
        NotificationAction(GoConstants.REWIND_ACTION, GoConstants.FAST_FORWARD_ACTION),
        NotificationAction(GoConstants.FAVORITE_ACTION, GoConstants.CLOSE_ACTION),
        NotificationAction(GoConstants.FAVORITE_POSITION_ACTION, GoConstants.CLOSE_ACTION)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CheckableItemsHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.notification_actions_item,
            parent,
            false
        )
    )

    override fun getItemCount() = mActions.size

    override fun onBindViewHolder(holder: CheckableItemsHolder, position: Int) {
        holder.bindItems()
    }

    inner class CheckableItemsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems() {

            with(itemView) {

                mediaPlayerHolder?.let { mph ->
                    findViewById<ImageView>(R.id.notif_action_0).setImageResource(
                        Theming.getNotificationActionIcon(mActions[absoluteAdapterPosition].first, mph, isNotification = false)
                    )
                    findViewById<ImageView>(R.id.notif_action_1).setImageResource(
                        Theming.getNotificationActionIcon(mActions[absoluteAdapterPosition].second, mph, isNotification = false)
                    )

                    val itemViewColor = itemView.solidColor
                    if (selectedActions == mActions[absoluteAdapterPosition]) {
                        itemView.setBackgroundColor(
                           ColorUtils.setAlphaComponent(Theming.resolveThemeColor(ctx.resources), 25)
                        )
                    } else {
                        itemView.setBackgroundColor(
                            itemViewColor
                        )
                    }

                    contentDescription = ctx.getString(Theming.getNotificationActionTitle(mActions[absoluteAdapterPosition].first))
                }

                setOnClickListener {
                    notifyItemChanged(mActions.indexOf(selectedActions))
                    selectedActions = mActions[absoluteAdapterPosition]
                    notifyItemChanged(absoluteAdapterPosition)
                    GoPreferences.getPrefsInstance().notificationActions = selectedActions
                }

                setOnLongClickListener {
                    Theming.getNotificationActionTitle(mActions[absoluteAdapterPosition].first).toToast(ctx)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
