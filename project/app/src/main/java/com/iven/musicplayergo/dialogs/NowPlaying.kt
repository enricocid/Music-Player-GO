package com.iven.musicplayergo.dialogs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.NowPlayingBinding
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.windowInsetTypesOf


class NowPlaying: BottomSheetDialogFragment() {

    private var _nowPlayingBinding: NowPlayingBinding? = null

    var onNowPlayingAdded: ((NowPlayingBinding?) -> Unit)? = null
    var onNowPlayingCancelled: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _nowPlayingBinding = NowPlayingBinding.inflate(inflater, container, false)
        return _nowPlayingBinding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onNowPlayingCancelled?.invoke()
        _nowPlayingBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            dialog?.window?.navigationBarColor = ContextCompat.getColor(requireActivity(),
                R.color.windowBackground)
            Insetter.builder()
                .padding(windowInsetTypesOf(navigationBars = true))
                .margin(windowInsetTypesOf(statusBars = true))
                .applyToView(view)
        }
        onNowPlayingAdded?.invoke(_nowPlayingBinding)
    }

    companion object {

        const val TAG_MODAL = "NP_BOTTOM_SHEET"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ModalSheet.
         */
        @JvmStatic
        fun newInstance() = NowPlaying()
    }
}
