package com.iven.musicplayergo.dialogs


import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
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
import com.iven.musicplayergo.player.MediaPlayerHolder
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
    private var mSheetType = GoConstants.ACCENT_TYPE

    // interfaces
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface
    var onQueueCancelled: (() -> Unit)? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_TYPE)?.let { which ->
            mSheetType = which
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

    override fun onDestroyView() {
        super.onDestroyView()
        onQueueCancelled?.invoke()
        _modalRvBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var dialogTitle = getString(R.string.accent_pref_title)

        _modalRvBinding?.run {

            modalRv.isVerticalScrollBarEnabled = false
            modalRv.isHorizontalScrollBarEnabled = false

            when (mSheetType) {

                GoConstants.ACCENT_TYPE -> {

                    // use alt RecyclerView
                    modalRv.visibility = View.GONE

                    val accentsAdapter = AccentsAdapter(requireActivity())
                    val layoutManager =  LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    modalRvAlt.layoutManager = layoutManager
                    modalRvAlt.adapter = accentsAdapter

                    // scroll to selected item
                    modalRvAlt.post {
                        layoutManager.scrollToPositionWithOffset(ThemeHelper.getAccentedTheme().second, 0)
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

                    dialogTitle = getString(R.string.filter_pref_title)

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

                    MediaPlayerHolder.getInstance().run {

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
                                        BottomSheetBehavior.from(bs).peekHeight = height
                                        val indexOfCurrentSong = queueSongs.indexOf(currentSong)
                                        val layoutManager = modalRv.layoutManager as LinearLayoutManager
                                        layoutManager.scrollToPositionWithOffset(indexOfCurrentSong, 0)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {

                    dialogTitle = getString(R.string.favorites)

                    modalRvAlt.visibility = View.GONE

                    _modalRvBinding?.btnContainer?.visibility = View.GONE

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
            dialog?.window?.navigationBarColor = ContextCompat.getColor(requireActivity(),
                R.color.windowBackground)
            Insetter.builder()
                .padding(windowInsetTypesOf(navigationBars = true))
                .margin(windowInsetTypesOf(statusBars = true))
                .applyToView(view)
        }
    }

    fun swapQueueSong(song: Music?) {
        if (::mQueueAdapter.isInitialized) {
            mQueueAdapter.swapSelectedSong(song)
        }
    }

    companion object {

        const val TAG_MODAL_RV = "MODAL_RV"
        private const val TAG_TYPE = "MODAL_RV_TYPE"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ModalSheet.
         */
        @JvmStatic
        fun newInstance(which: String) = RecyclerSheet().apply {
            arguments = bundleOf(TAG_TYPE to which)
        }
    }
}
