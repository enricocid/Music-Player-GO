package com.iven.musicplayergo.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.NowPlayingBinding
import com.iven.musicplayergo.databinding.NowPlayingControlsBinding
import com.iven.musicplayergo.databinding.NowPlayingExtendedControlsBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.player.MediaPlayerHolder
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge


class NowPlayingBottomSheet : BottomSheetDialogFragment() {

    // Now playing
    private lateinit var mNowPlayingBinding: NowPlayingBinding
    private lateinit var mNowPlayingControlsBinding: NowPlayingControlsBinding
    private lateinit var mNowPlayingExtendedControlsBinding: NowPlayingExtendedControlsBinding

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    // Colors
    private val mResolvedAccentColor get() = ThemeHelper.resolveThemeAccent(requireContext())

    private val mResolvedIconsColor get() = R.color.widgetsColor.decodeColor(requireContext())
    private val mResolvedDisabledIconsColor
        get() = ThemeHelper.resolveColorAttr(
            requireContext(),
            android.R.attr.colorButtonNormal
        )

    private val sLandscape get() = ThemeHelper.isDeviceLand(resources)

    private var sUpdatePlayerProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val nowPlayingView = inflater.inflate(R.layout.now_playing, container, false)

        mNowPlayingBinding = NowPlayingBinding.bind(nowPlayingView)
        mNowPlayingControlsBinding = NowPlayingControlsBinding.bind(mNowPlayingBinding.root)
        mNowPlayingExtendedControlsBinding =
            NowPlayingExtendedControlsBinding.bind(mNowPlayingBinding.root)

        initViews()

        mNowPlayingBinding.npPlayingInfo.afterMeasured {
            val params = layoutParams as LinearLayout.LayoutParams
            val marginTop = if (goPreferences.isEdgeToEdge || sLandscape) {
                0
            } else {
                resources.getDimensionPixelOffset(R.dimen.md_corner_radius)
            }
            params.topMargin = marginTop
            layoutParams = params
        }
        mMediaPlayerHolder.mediaPlayerInterface?.onBottomSheetCreated(this)
        return nowPlayingView
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mMediaPlayerHolder.mediaPlayerInterface?.onDismissNP()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNowPlayingControlsBinding.npRepeat.setImageResource(
            ThemeHelper.getRepeatIcon(
                mMediaPlayerHolder
            )
        )

        ThemeHelper.updateIconTint(
            mNowPlayingControlsBinding.npRepeat,
            if (mMediaPlayerHolder.isRepeat1X || mMediaPlayerHolder.isLooping!!) {
                mResolvedAccentColor
            } else {
                mResolvedIconsColor
            }
        )

        if (goPreferences.isPreciseVolumeEnabled) {
            setupPreciseVolumeHandler()
        } else {
            mNowPlayingExtendedControlsBinding.npVolumeSeek.isEnabled = false
            ThemeHelper.updateIconTint(
                mNowPlayingExtendedControlsBinding.npVolume,
                mResolvedDisabledIconsColor
            )
        }

        setSeekBarProgressListener()

        updateNowPlayingInfo()

        mNowPlayingBinding.npSeekBar.progress =
            mMediaPlayerHolder.playerPosition!!


    }

    override fun onStart() {
        super.onStart()
        if (goPreferences.isEdgeToEdge) {
            dialog?.window?.applyEdgeToEdgeBottomSheet(resources)
            if (sLandscape) {
                edgeToEdge { dialog?.window?.decorView?.rootView?.fit { Edge.Bottom + Edge.Top + Edge.Right } }
            }
        }
    }

    private fun initViews() {

        val mediaPlayerInterface = mMediaPlayerHolder.mediaPlayerInterface

        mNowPlayingBinding.npSong.isSelected = true
        mNowPlayingBinding.npArtistAlbum.isSelected = true

        mNowPlayingBinding.npPlayingInfo.setOnClickListener {
            synchronized(dismissAllowingStateLoss()) {
                mediaPlayerInterface?.onOpenPlayingArtistAlbumNP()
            }
        }

        mNowPlayingControlsBinding.npSkipPrev.setOnClickListener {
            mediaPlayerInterface?.onSkipNP(
                false
            )
        }

        mNowPlayingControlsBinding.npEq.setOnClickListener {
            mMediaPlayerHolder.openEqualizer(
                requireActivity()
            )
        }
        mNowPlayingControlsBinding.npPlay.setOnClickListener { mMediaPlayerHolder.resumeOrPause() }

        mNowPlayingControlsBinding.npSkipNext.setOnClickListener {
            mediaPlayerInterface?.onSkipNP(
                true
            )
        }
        mNowPlayingControlsBinding.npRepeat.setOnClickListener { mediaPlayerInterface?.onSetRepeatNP() }
        mNowPlayingExtendedControlsBinding.npLove.setOnClickListener {
            ListsHelper.addToLovedSongs(
                requireContext(),
                mMediaPlayerHolder.currentSong.first,
                mMediaPlayerHolder.playerPosition!!,
                mMediaPlayerHolder.launchedBy
            )
            mediaPlayerInterface?.onLovedSongUpdate(false)
        }
    }

    fun updateProgress(position: Int) {
        sUpdatePlayerProgress = false
        mNowPlayingBinding.npSeekBar.progress = position
    }

    fun updateNowPlayingInfo() {

        val selectedSong = mMediaPlayerHolder.currentSong.first
        val selectedSongDuration = selectedSong?.duration!!

        mNowPlayingBinding.npSong.text = selectedSong.title

        mNowPlayingBinding.npArtistAlbum.text =
            getString(
                R.string.artist_and_album,
                selectedSong.artist,
                selectedSong.album
            )

        mNowPlayingBinding.npSeek.text =
            mMediaPlayerHolder.playerPosition!!.toLong()
                .toFormattedDuration(false, isSeekBar = true)
        mNowPlayingBinding.npDuration.text =
            selectedSongDuration.toFormattedDuration(false, isSeekBar = true)

        mNowPlayingBinding.npSeekBar.max = selectedSong.duration.toInt()

        selectedSong.id?.toContentUri()?.toBitrate(requireContext())?.let { bitrateInfo ->
            mNowPlayingBinding.npRates.text =
                getString(R.string.rates, bitrateInfo.first, bitrateInfo.second)
        }

        updatePlayingStatus()
    }

    private fun setSeekBarProgressListener() {

        mNowPlayingBinding.npSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                val defaultPositionColor = mNowPlayingBinding.npSeek.currentTextColor
                var userSelectedPosition = 0
                var isUserSeeking = false

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (isUserSeeking) {
                        userSelectedPosition = progress
                        mNowPlayingBinding.npSeek.setTextColor(
                            mResolvedAccentColor
                        )
                    }
                    mNowPlayingBinding.npSeek.text =
                        progress.toLong().toFormattedDuration(isAlbum = false, isSeekBar = true)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isUserSeeking) {
                        mNowPlayingBinding.npSeek.setTextColor(defaultPositionColor)
                        isUserSeeking = false
                    }
                    if (mMediaPlayerHolder.state != GoConstants.PLAYING) {
                        if (sUpdatePlayerProgress) {
                            mMediaPlayerHolder.mediaPlayerInterface?.onPositionChanged(
                                userSelectedPosition
                            )
                        } else {
                            sUpdatePlayerProgress = true
                        }
                        mNowPlayingBinding.npSeekBar.progress = userSelectedPosition
                    }
                    mMediaPlayerHolder.seekTo(
                        userSelectedPosition,
                        updatePlaybackStatus = mMediaPlayerHolder.state != GoConstants.PAUSED,
                        restoreProgressCallBack = !isUserSeeking
                    )
                }
            })
    }

    fun updateRepeatStatus(onPlaybackCompletion: Boolean) {
        mNowPlayingControlsBinding.npRepeat.setImageResource(
            ThemeHelper.getRepeatIcon(
                mMediaPlayerHolder
            )
        )
        when {
            onPlaybackCompletion -> ThemeHelper.updateIconTint(
                mNowPlayingControlsBinding.npRepeat,
                mResolvedIconsColor
            )
            mMediaPlayerHolder.isRepeat1X or mMediaPlayerHolder.isLooping!! -> {
                ThemeHelper.updateIconTint(
                    mNowPlayingControlsBinding.npRepeat,
                    mResolvedAccentColor
                )
            }
            else -> ThemeHelper.updateIconTint(
                mNowPlayingControlsBinding.npRepeat,
                mResolvedIconsColor
            )
        }
    }

    private fun setupPreciseVolumeHandler() {

        mMediaPlayerHolder.currentVolumeInPercent.apply {
            mNowPlayingExtendedControlsBinding.npVolume.setImageResource(
                ThemeHelper.getPreciseVolumeIcon(
                    this
                )
            )
            mNowPlayingExtendedControlsBinding.npVolumeSeek.progress = this
        }

        mNowPlayingExtendedControlsBinding.npVolumeSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {

            var isUserSeeking = false

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                if (isUserSeeking) {

                    mMediaPlayerHolder.setPreciseVolume(progress)

                    mNowPlayingExtendedControlsBinding.npVolume.setImageResource(
                        ThemeHelper.getPreciseVolumeIcon(
                            progress
                        )
                    )

                    ThemeHelper.updateIconTint(
                        mNowPlayingExtendedControlsBinding.npVolume,
                        mResolvedAccentColor
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                ThemeHelper.updateIconTint(
                    mNowPlayingExtendedControlsBinding.npVolume,
                    mResolvedIconsColor
                )
            }
        })
    }

    fun updatePlayingStatus() {
        val isPlaying = mMediaPlayerHolder.state != GoConstants.PAUSED
        val drawable =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        mNowPlayingControlsBinding.npPlay.setImageResource(drawable)
    }

    companion object {

        const val TAG_NOW_PLAYING = "TAG_NOW_PLAYING"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment NowPlayingBottomSheet.
         */
        @JvmStatic
        fun newInstance() = NowPlayingBottomSheet()
    }
}
