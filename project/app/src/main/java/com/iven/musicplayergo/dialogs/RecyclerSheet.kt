package com.iven.musicplayergo.dialogs


import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.ModalRvBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.preferences.AccentsAdapter
import com.iven.musicplayergo.preferences.ActiveTabsAdapter
import com.iven.musicplayergo.preferences.FiltersAdapter
import com.iven.musicplayergo.ui.ItemSwipeCallback
import com.iven.musicplayergo.ui.ItemTouchCallback
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.windowInsetTypesOf


class RecyclerSheet: BottomSheetDialogFragment() {

    private var _modalRvBinding: ModalRvBinding? = null
    private lateinit var mQueueAdapter: QueueAdapter

    // sheet type
    var sheetType = GoConstants.ACCENT_TYPE

    // interfaces
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface
    var onQueueCancelled: (() -> Unit)? = null
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
        _modalRvBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var dialogTitle = getString(R.string.accent_pref_title)

        _modalRvBinding?.run {

            when (sheetType) {

                GoConstants.ACCENT_TYPE -> {

                    // use alt RecyclerView
                    modalRv.visibility = View.GONE
                    sleepTimerElapsed.visibility = View.GONE

                    modalRvAlt.setHasFixedSize(true)
                    val accentsAdapter = AccentsAdapter(requireActivity())
                    val layoutManager =  LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRvAlt.layoutManager = layoutManager
                    modalRvAlt.adapter = accentsAdapter

                    ThemeHelper.getAccentedTheme(resources)?.run {
                        modalRvAlt.post { layoutManager.scrollToPositionWithOffset(second, 0) }
                    }

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        goPreferences.accent = accentsAdapter.selectedAccent
                        mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                    }
                }

                GoConstants.TABS_TYPE -> {

                    dialogTitle = getString(R.string.active_fragments_pref_title)

                    modalRv.visibility = View.GONE
                    sleepTimerElapsed.visibility = View.GONE

                    modalRvAlt.setHasFixedSize(true)
                    val activeTabsAdapter = ActiveTabsAdapter(requireActivity())
                    modalRvAlt.adapter = activeTabsAdapter

                    val touchHelper = ItemTouchHelper(ItemTouchCallback(activeTabsAdapter.availableItems, isActiveTabs = true))
                    touchHelper.attachToRecyclerView(modalRvAlt)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = activeTabsAdapter.getUpdatedItems()
                        if (goPreferences.activeTabs != updatedItems) {
                            goPreferences.activeTabs = updatedItems
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                        } else {
                            dismiss()
                        }
                    }
                }

                GoConstants.FILTERS_TYPE -> {

                    modalRv.visibility = View.GONE
                    sleepTimerElapsed.visibility = View.GONE

                    dialogTitle = getString(R.string.filter_pref_title)

                    modalRvAlt.setHasFixedSize(true)
                    val filtersAdapter = FiltersAdapter(requireActivity())
                    modalRvAlt.adapter = filtersAdapter

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = filtersAdapter.getUpdatedItems()
                        if (goPreferences.filters != updatedItems) {
                            goPreferences.filters = updatedItems
                            mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
                        } else {
                            dismiss()
                        }
                    }
                }

                GoConstants.QUEUE_TYPE -> {

                    dialogTitle = getString(R.string.queue)

                    modalRvAlt.visibility = View.GONE
                    _modalRvBinding?.btnContainer?.visibility = View.GONE
                    sleepTimerElapsed.visibility = View.GONE

                    setRecyclerViewProps(modalRv)

                    mMediaControlInterface.onGetMediaPlayerHolder()?.run {

                        mQueueAdapter = QueueAdapter(requireActivity(), this)
                        modalRv.adapter = mQueueAdapter
                        if (ThemeHelper.isDeviceLand(resources)) {
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
                                // to ensure full dialog's height
                                _modalRvBinding?.root?.afterMeasured {
                                    dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bs ->
                                        BottomSheetBehavior.from(bs).state = BottomSheetBehavior.STATE_EXPANDED
                                        val indexOfCurrentSong = queueSongs.indexOf(currentSong)
                                        val layoutManager = modalRv.layoutManager as LinearLayoutManager
                                        layoutManager.scrollToPositionWithOffset(indexOfCurrentSong, 0)
                                    }
                                }
                            }
                        }
                    }
                }

                GoConstants.SLEEPTIMER_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    val sleepTimerAdapter = SleepTimerAdapter()
                    modalRv.visibility = View.GONE
                    modalRvAlt.setHasFixedSize(true)
                    modalRvAlt.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRvAlt.adapter = sleepTimerAdapter
                    sleepTimerElapsed.visibility = View.GONE

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
                            val isEnabled =pauseBySleepTimer(sleepTimerAdapter.getSelectedSleepTimerValue(), sleepTimerAdapter.getSelectedSleepTimer())
                            onSleepTimerEnabled?.invoke(isEnabled)
                        }
                        dismiss()
                    }
                }

                GoConstants.SLEEPTIMER_ELAPSED_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer_remaining_time)

                    modalRv.visibility = View.GONE
                    modalRvAlt.visibility = View.GONE

                    btnNegative.setOnClickListener {
                        dismiss()
                    }

                    btnPositive.run {
                        text = getString(R.string.sleeptimer_stop)
                        contentDescription = getString(R.string.sleeptimer_cancel_desc)
                        setOnClickListener {
                            mMediaControlInterface.onGetMediaPlayerHolder()?.cancelSleepTimer()
                            onSleepTimerEnabled?.invoke(false)
                            dismiss()
                        }
                    }
                }

                else -> {

                    dialogTitle = getString(R.string.favorites)

                    modalRvAlt.visibility = View.GONE
                    sleepTimerElapsed.visibility = View.GONE

                    _modalRvBinding?.btnContainer?.visibility = View.GONE

                    setRecyclerViewProps(modalRv)
                    val favoritesAdapter = FavoritesAdapter(requireActivity())
                    modalRv.adapter = favoritesAdapter
                    if (ThemeHelper.isDeviceLand(resources)) {
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
                            mMediaControlInterface.onAddToQueue(goPreferences.favorites?.get(index))
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            dialog?.window?.navigationBarColor =
                ThemeHelper.resolveColorAttr(requireContext(), R.attr.main_bg)
            Insetter.builder()
                .padding(windowInsetTypesOf(navigationBars = true))
                .margin(windowInsetTypesOf(statusBars = true))
                .applyToView(view)
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
            ThemeHelper.resolveColorAttr(requireActivity(), android.R.attr.textColorPrimary)

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

                (itemView as TextView).run {
                    text = itemSleepOption
                    contentDescription = itemSleepOption
                    setTextColor(if (mSelectedPosition == absoluteAdapterPosition) {
                        ThemeHelper.resolveThemeAccent(requireActivity())
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
