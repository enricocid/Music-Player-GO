package com.iven.musicplayergo.dialogs


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.ModalRvBinding
import com.iven.musicplayergo.databinding.SleeptimerItemBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
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
    var onSleepTimerEnabled: ((Boolean, String) -> Unit)? = null

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    private val mGoPreferences get() = GoPreferences.getPrefsInstance()

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

        dialog?.disableShapeAnimation()
        dialog?.applyFullHeightDialog(requireActivity())

        var dialogTitle = getString(R.string.accent_pref_title)

        _modalRvBinding?.run {

            when (sheetType) {

                ACCENT_TYPE -> {

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    val accentsAdapter = AccentsAdapter(resources.getIntArray(R.array.colors))
                    val layoutManager =  LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)

                    modalRv.layoutManager = layoutManager
                    modalRv.adapter = accentsAdapter
                    modalRv.setHasFixedSize(true)

                    modalRv.post {
                        layoutManager.scrollToPositionWithOffset(mGoPreferences.accent, 0)
                    }

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mGoPreferences.accent = accentsAdapter.selectedAccent
                        mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                    }
                }

                TABS_TYPE -> {

                    dialogTitle = getString(R.string.active_fragments_pref_title)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    val activeTabsAdapter = ActiveTabsAdapter()
                    modalRv.adapter = activeTabsAdapter
                    modalRv.setHasFixedSize(true)

                    val touchHelper = ItemTouchHelper(ItemTouchCallback(activeTabsAdapter.availableItems, isActiveTabs = true))
                    touchHelper.attachToRecyclerView(modalRv)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = activeTabsAdapter.getUpdatedItems()
                        updatedItems.takeIf { it != mGoPreferences.activeTabs}?.let { updatedList ->
                            mGoPreferences.activeTabs = updatedList
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                            return@setOnClickListener
                        }
                        dismiss()
                    }
                }

                FILTERS_TYPE -> {

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    dialogTitle = getString(R.string.filter_pref_title)

                    val filtersAdapter = FiltersAdapter()
                    modalRv.adapter = filtersAdapter
                    modalRv.setHasFixedSize(true)

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
                        updatedItems.takeIf { it != mGoPreferences.filters}?.let { updatedList ->
                            mGoPreferences.filters = updatedList
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                            return@setOnClickListener
                        }
                        dismiss()
                    }
                }

                QUEUE_TYPE -> {

                    dialogTitle = getString(R.string.queue)

                    _modalRvBinding?.btnContainer?.handleViewVisibility(show = false)
                    sleepTimerElapsed.handleViewVisibility(show = false)

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        Dialogs.showClearQueueDialog(requireContext())
                    }

                    modalRv.disableScrollbars()

                    FastScrollerBuilder(modalRv).useMd2Style().build()

                    with(mMediaPlayerHolder) {

                        mQueueAdapter = QueueAdapter()
                        modalRv.adapter = mQueueAdapter
                        if (Theming.isDeviceLand(resources)) modalRv.layoutManager = GridLayoutManager(context, 3)

                        mQueueAdapter.onQueueCleared = {
                            dismiss()
                        }

                        ItemTouchHelper(ItemTouchCallback(mQueueAdapter.queueSongs, isActiveTabs = false))
                            .attachToRecyclerView(modalRv)
                        ItemTouchHelper(ItemSwipeCallback(isQueueDialog = true, isFavoritesDialog = false) { viewHolder: RecyclerView.ViewHolder, _: Int ->
                            if (!mQueueAdapter.performQueueSongDeletion(
                                    requireActivity(),
                                    viewHolder.absoluteAdapterPosition)
                            ) {
                                mQueueAdapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                            }
                        }).attachToRecyclerView(modalRv)

                        modalRv.post {
                            if (isQueueStarted) {
                                val indexOfCurrentSong = queueSongs.findIndex(currentSong)
                                val layoutManager = modalRv.layoutManager as LinearLayoutManager
                                layoutManager.scrollToPositionWithOffset(indexOfCurrentSong, 0)
                            }
                        }
                    }
                }

                SLEEPTIMER_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    val sleepTimerAdapter = SleepTimerAdapter()
                    modalRv.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRv.adapter = sleepTimerAdapter
                    modalRv.setHasFixedSize(true)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        with(mMediaPlayerHolder) {
                            val isEnabled = pauseBySleepTimer(sleepTimerAdapter.getSelectedSleepTimerValue())
                            onSleepTimerEnabled?.invoke(isEnabled, sleepTimerAdapter.getSelectedSleepTimer())
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
                            mMediaPlayerHolder.cancelSleepTimer()
                            onSleepTimerEnabled?.invoke(false, "")
                            dismiss()
                        }
                    }
                }

                NOTIFICATION_ACTIONS_TYPE -> {

                    dialogTitle = getString(R.string.notification_actions_pref_title)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    modalRv.setHasFixedSize(true)
                    val notificationActionsAdapter = NotificationActionsAdapter()
                    val layoutManager =  LinearLayoutManager(requireActivity())
                    modalRv.layoutManager = layoutManager
                    modalRv.adapter = notificationActionsAdapter
                    modalRv.setHasFixedSize(true)

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaPlayerHolder.onHandleNotificationUpdate(
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

                    modalRv.disableScrollbars()
                    val favoritesAdapter = FavoritesAdapter()
                    modalRv.adapter = favoritesAdapter
                    FastScrollerBuilder(modalRv).useMd2Style().build()

                    if (Theming.isDeviceLand(resources)) {
                        modalRv.layoutManager = GridLayoutManager(context, 3)
                    }

                    with(favoritesAdapter) {
                        onFavoritesCleared = {
                            dismiss()
                        }
                        onFavoritesUpdate = {
                            mUIControlInterface.onFavoritesUpdated(clear = false)
                            mUIControlInterface.onFavoriteAddedOrRemoved()
                        }
                        onFavoriteQueued = { favoriteToQueue ->
                            mMediaControlInterface.onAddToQueue(favoriteToQueue)
                        }
                        onFavoritesQueued = { favorites, forcePlay ->
                            mMediaControlInterface.onAddAlbumToQueue(favorites, forcePlay = forcePlay)
                        }
                    }

                    ItemTouchHelper(ItemSwipeCallback(isQueueDialog = false, isFavoritesDialog = true) {
                            viewHolder: RecyclerView.ViewHolder,
                            direction: Int ->
                        val index = viewHolder.absoluteAdapterPosition
                        favoritesAdapter.notifyItemChanged(index)
                        if (direction == ItemTouchHelper.RIGHT) {
                            mMediaControlInterface.onAddToQueue(mGoPreferences.favorites?.get(index))
                            return@ItemSwipeCallback
                        }
                        favoritesAdapter.performFavoriteDeletion(requireActivity(), index)
                    }).attachToRecyclerView(modalRv)
                }
            }
            // finally, set the sheet's title
            title.text = dialogTitle
        }
    }

    fun swapQueueSong(song: Music?) {
        if (::mQueueAdapter.isInitialized) mQueueAdapter.swapSelectedSong(song)
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SleepTimerHolder {
            val binding = SleeptimerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SleepTimerHolder(binding)
        }

        override fun getItemCount(): Int {
            return sleepOptions.size
        }

        override fun onBindViewHolder(holder: SleepTimerHolder, position: Int) {
            holder.bindItems(sleepOptions[holder.absoluteAdapterPosition])
        }

        inner class SleepTimerHolder(private val binding: SleeptimerItemBinding): RecyclerView.ViewHolder(binding.root) {

            fun bindItems(itemSleepOption: String) {

                with(binding.root) {
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
