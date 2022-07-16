package com.iven.musicplayergo.dialogs


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.ModalRvBinding
import com.iven.musicplayergo.extensions.applyFullScreenBottomSheetBehaviour
import com.iven.musicplayergo.extensions.handleViewVisibility
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.preferences.AccentsAdapter
import com.iven.musicplayergo.preferences.ActiveTabsAdapter
import com.iven.musicplayergo.preferences.FiltersAdapter
import com.iven.musicplayergo.preferences.NotificationActionsAdapter
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.ui.ItemTouchCallback
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder


class RecyclerSheet: BottomSheetDialogFragment() {

    private var _modalRvBinding: ModalRvBinding? = null
    private lateinit var mQueueAdapter: QueueAdapter

    // sheet type
    var sheetType = ACCENT_TYPE

    // interfaces
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface
    var onQueueCancelled: (() -> Unit)? = null
    var onFavoritesDialogCancelled: (() -> Unit)? = null
    var onSleepTimerDialogCancelled: (() -> Unit)? = null
    var onSleepTimerEnabled: ((Boolean) -> Unit)? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_TYPE)?.let { which ->
            sheetType = which
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _modalRvBinding = ModalRvBinding.inflate(inflater, container, false)
        return _modalRvBinding?.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onSleepTimerDialogCancelled?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onQueueCancelled?.invoke()
        onFavoritesDialogCancelled?.invoke()
        onSleepTimerDialogCancelled?.invoke()
        _modalRvBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().applyFullScreenBottomSheetBehaviour(dialog)

        var dialogTitle = getString(R.string.accent_pref_title)

        _modalRvBinding?.run {

            when (sheetType) {

                ACCENT_TYPE -> {

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    modalRv.setHasFixedSize(true)
                    val accentsAdapter = AccentsAdapter(requireActivity())
                    val layoutManager =  LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRv.layoutManager = layoutManager
                    modalRv.adapter = accentsAdapter

                    modalRv.post {
                        layoutManager.scrollToPositionWithOffset(
                            GoPreferences.getPrefsInstance().accent,
                            0
                        )
                    }

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        GoPreferences.getPrefsInstance().accent = accentsAdapter.selectedAccent
                        mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                    }
                }

                TABS_TYPE -> {

                    dialogTitle = getString(R.string.active_fragments_pref_title)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    modalRv.setHasFixedSize(true)
                    val activeTabsAdapter = ActiveTabsAdapter(requireActivity())
                    modalRv.adapter = activeTabsAdapter

                    val touchHelper = ItemTouchHelper(ItemTouchCallback(activeTabsAdapter.availableItems, isActiveTabs = true))
                    touchHelper.attachToRecyclerView(modalRv)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = activeTabsAdapter.getUpdatedItems()
                        if (GoPreferences.getPrefsInstance().activeTabs != updatedItems) {
                            GoPreferences.getPrefsInstance().activeTabs = updatedItems
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                        } else {
                            dismiss()
                        }
                    }
                }

                FILTERS_TYPE -> {

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    dialogTitle = getString(R.string.filter_pref_title)

                    modalRv.setHasFixedSize(true)
                    val filtersAdapter = FiltersAdapter(requireActivity())
                    modalRv.adapter = filtersAdapter

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        Dialogs.showClearFiltersDialog(requireActivity())
                    }

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = filtersAdapter.getUpdatedItems()
                        if (GoPreferences.getPrefsInstance().filters != updatedItems) {
                            GoPreferences.getPrefsInstance().filters = updatedItems
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                        } else {
                            dismiss()
                        }
                    }
                }

                QUEUE_TYPE -> {

                    dialogTitle = getString(R.string.queue)

                    _modalRvBinding?.btnContainer?.handleViewVisibility(show = false)
                    sleepTimerElapsed.handleViewVisibility(show = false)

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        mMediaControlInterface.onGetMediaPlayerHolder()?.let { mph ->
                            Dialogs.showClearQueueDialog(requireContext(), mph)
                        }
                    }

                    setRecyclerViewProps(modalRv)

                    FastScrollerBuilder(modalRv).useMd2Style().build()

                    mMediaControlInterface.onGetMediaPlayerHolder()?.run {

                        mQueueAdapter = QueueAdapter(requireActivity(), this)
                        modalRv.adapter = mQueueAdapter
                        if (Theming.isDeviceLand(resources)) {
                            modalRv.layoutManager = GridLayoutManager(context, 3)
                        }

                        mQueueAdapter.onQueueCleared = {
                            dismiss()
                        }

                        ItemTouchHelper(ItemTouchCallback(mQueueAdapter.queueSongs, isActiveTabs = false))
                            .attachToRecyclerView(modalRv)
                        ItemTouchHelper(ItemSwipeCallback(requireActivity(), isQueueDialog = true, isFavoritesDialog = false) { viewHolder: RecyclerView.ViewHolder, _: Int ->
                            if (!mQueueAdapter.performQueueSongDeletion(viewHolder.absoluteAdapterPosition)) {
                                mQueueAdapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                            }
                        }).attachToRecyclerView(modalRv)

                        modalRv.post {
                            if (isQueueStarted) {
                                val indexOfCurrentSong = queueSongs.indexOf(currentSong)
                                val layoutManager = modalRv.layoutManager as LinearLayoutManager
                                layoutManager.scrollToPositionWithOffset(indexOfCurrentSong, 0)
                            }
                        }
                    }
                }

                SLEEPTIMER_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    val sleepTimerAdapter = SleepTimerAdapter()
                    modalRv.setHasFixedSize(true)
                    modalRv.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRv.adapter = sleepTimerAdapter
                    sleepTimerElapsed.handleViewVisibility(show = false)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
                            val isEnabled = pauseBySleepTimer(sleepTimerAdapter.getSelectedSleepTimerValue(), sleepTimerAdapter.getSelectedSleepTimer())
                            onSleepTimerEnabled?.invoke(isEnabled)
                        }
                        dismiss()
                    }
                }

                SLEEPTIMER_ELAPSED_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    modalRv.handleViewVisibility(show = false)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }

                    with(btnPositive) {
                        text = getString(R.string.sleeptimer_stop)
                        contentDescription = getString(R.string.sleeptimer_cancel_desc)
                        setOnClickListener {
                            mMediaControlInterface.onGetMediaPlayerHolder()?.cancelSleepTimer()
                            onSleepTimerEnabled?.invoke(false)
                            dismiss()
                        }
                    }
                }

                NOTIFICATION_ACTIONS_TYPE -> {

                    dialogTitle = getString(R.string.notification_actions_pref_title)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    modalRv.setHasFixedSize(true)
                    val notificationActionsAdapter = NotificationActionsAdapter(requireContext(), mMediaControlInterface.onGetMediaPlayerHolder())
                    val layoutManager =  LinearLayoutManager(requireActivity())
                    modalRv.layoutManager = layoutManager
                    modalRv.adapter = notificationActionsAdapter

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaControlInterface.onGetMediaPlayerHolder()?.onHandleNotificationUpdate(
                            isAdditionalActionsChanged = true
                        )
                        dismiss()
                    }
                }

                else -> {

                    dialogTitle = getString(R.string.favorites)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    _modalRvBinding?.btnContainer?.handleViewVisibility(show = false)

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        Dialogs.showClearFavoritesDialog(requireActivity())
                    }

                    setRecyclerViewProps(modalRv)
                    val favoritesAdapter = FavoritesAdapter(requireActivity())
                    modalRv.adapter = favoritesAdapter
                    FastScrollerBuilder(modalRv).useMd2Style().build()

                    if (Theming.isDeviceLand(resources)) {
                        modalRv.layoutManager = GridLayoutManager(context, 3)
                    }
                    favoritesAdapter.onFavoritesCleared = {
                        dismiss()
                    }

                    ItemTouchHelper(ItemSwipeCallback(requireActivity(),
                        isQueueDialog = false, isFavoritesDialog = true) {
                            viewHolder: RecyclerView.ViewHolder,
                            direction: Int ->
                        val index = viewHolder.absoluteAdapterPosition
                        if (direction == ItemTouchHelper.RIGHT) {
                            mMediaControlInterface.onAddToQueue(GoPreferences.getPrefsInstance().favorites?.get(index))
                        } else {
                            favoritesAdapter.performFavoriteDeletion(index)
                        }
                        favoritesAdapter.notifyItemChanged(index)
                    }).attachToRecyclerView(modalRv)
                }
            }
            // finally, set the sheet's title
            title.text = dialogTitle
        }
    }

    private fun setRecyclerViewProps(rv: RecyclerView) {
        rv.isVerticalScrollBarEnabled = false
        rv.isHorizontalScrollBarEnabled = false
        rv.setHasFixedSize(true)
    }

    fun swapQueueSong(song: Music?) {
        if (::mQueueAdapter.isInitialized) {
            mQueueAdapter.swapSelectedSong(song)
        }
    }

    fun updateCountdown(value: String) {
        _modalRvBinding?.sleepTimerElapsed?.text = value
    }

    private inner class SleepTimerAdapter : RecyclerView.Adapter<SleepTimerAdapter.SleepTimerHolder>() {

        private val sleepOptions = resources.getStringArray(R.array.sleepOptions)
        private val sleepOptionValues = resources.getIntArray(R.array.sleepOptionsValues)

        private var mSelectedPosition = 0

        private val mDefaultTextColor =
            Theming.resolveColorAttr(requireActivity(), android.R.attr.textColorPrimary)

        fun getSelectedSleepTimer(): String = sleepOptions[mSelectedPosition]
        fun getSelectedSleepTimerValue() = sleepOptionValues[mSelectedPosition].toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SleepTimerHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.sleeptimer_item,
                parent,
                false
            )
        )

        override fun getItemCount(): Int {
            return sleepOptions.size
        }

        override fun onBindViewHolder(holder: SleepTimerHolder, position: Int) {
            holder.bindItems(sleepOptions[holder.absoluteAdapterPosition])
        }

        inner class SleepTimerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(itemSleepOption: String) {

                with(itemView as TextView) {
                    text = itemSleepOption
                    contentDescription = itemSleepOption
                    setTextColor(if (mSelectedPosition == absoluteAdapterPosition) {
                        Theming.resolveThemeColor(resources)
                    } else {
                        mDefaultTextColor
                    })
                    setOnClickListener {
                        notifyItemChanged(mSelectedPosition)
                        mSelectedPosition = absoluteAdapterPosition
                        notifyItemChanged(mSelectedPosition)
                    }
                }
            }
        }
    }

    companion object {

        const val TAG_MODAL_RV = "MODAL_RV"
        private const val TAG_TYPE = "MODAL_RV_TYPE"

        // Modal rv type
        const val ACCENT_TYPE = "MODAL_ACCENT"
        const val TABS_TYPE = "MODAL_TABS"
        const val FILTERS_TYPE = "MODAL_FILTERS"
        const val QUEUE_TYPE = "MODAL_QUEUE"
        const val FAV_TYPE = "MODAL_FAVORITES"
        const val SLEEPTIMER_TYPE = "MODAL_SLEEPTIMER"
        const val SLEEPTIMER_ELAPSED_TYPE = "MODAL_SLEEPTIMER_ELAPSED"
        const val NOTIFICATION_ACTIONS_TYPE = "MODAL_NOTIFICATION_ACTIONS"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment [RecyclerSheet].
         */
        @JvmStatic
        fun newInstance(which: String) = RecyclerSheet().apply {
            arguments = bundleOf(TAG_TYPE to which)
        }
    }
}
