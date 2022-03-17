package com.iven.musicplayergo.dialogs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.NowPlayingBinding
import com.iven.musicplayergo.databinding.NowPlayingControlsBinding
import com.iven.musicplayergo.databinding.NowPlayingCoverBinding
import com.iven.musicplayergo.databinding.NowPlayingExtendedControlsBinding
import com.iven.musicplayergo.extensions.*
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.windowInsetTypesOf


class NowPlaying: BottomSheetDialogFragment() {

    private var _nowPlayingBinding: NowPlayingBinding? = null
    private var _npCoverBinding: NowPlayingCoverBinding? = null
    private var _npControlsBinding: NowPlayingControlsBinding? = null
    private var _npExtControlsBinding: NowPlayingExtendedControlsBinding? = null

    var onNowPlayingCancelled: (() -> Unit)? = null

    private var mAlbumIdNp : Long? = -1L

    private lateinit var mMediaControlInterface: MediaControlInterface
    private lateinit var mUIControlInterface: UIControlInterface

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mMediaControlInterface = activity as MediaControlInterface
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _nowPlayingBinding = NowPlayingBinding.inflate(inflater, container, false).apply {
            _npCoverBinding = NowPlayingCoverBinding.bind(root)
            _npControlsBinding = NowPlayingControlsBinding.bind(root)
            _npExtControlsBinding =
                NowPlayingExtendedControlsBinding.bind(root)
        }
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

        setupView()
    }

    private fun setupView() {
        _nowPlayingBinding?.run {
            npSong.isSelected = true
            npArtistAlbum.isSelected = true
            setupNPCoverLayout()
            npPlayingSongContainer.setOnClickListener {
                mUIControlInterface.onOpenPlayingArtistAlbum()
                dismiss()
            }
        }

        _npControlsBinding?.run {
            npSkipPrev.setOnClickListener { skip(isNext = false) }
            npFastRewind.setOnClickListener { mMediaControlInterface.onGetMediaPlayerHolder()?.fastSeek(isForward = false) }
            npPlay.setOnClickListener { mMediaControlInterface.onGetMediaPlayerHolder()?.resumeOrPause() }
            npSkipNext.setOnClickListener { skip(isNext = true) }
            npFastForward.setOnClickListener { mMediaControlInterface.onGetMediaPlayerHolder()?.fastSeek(isForward = true) }
        }

        setupPreciseVolumeHandler()
        setupSeekBarProgressListener()
        updateNpInfo()

        val mediaPlayerHolder = mMediaControlInterface.onGetMediaPlayerHolder()

        mediaPlayerHolder?.currentSong?.let { song ->
            loadNpCover(song)
            _nowPlayingBinding?.npSeek?.text =
                mediaPlayerHolder.playerPosition.toLong()
                    .toFormattedDuration(isAlbum = false, isSeekBar = true)

            _nowPlayingBinding?.npSeekBar?.progress = mediaPlayerHolder.playerPosition

            // to ensure full dialog's height
            _nowPlayingBinding?.root?.afterMeasured {
                dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.let { bs ->
                        BottomSheetBehavior.from(bs).peekHeight = height
                    }
            }
        }
    }

    private fun setupNPCoverLayout() {

        if (!ThemeHelper.isDeviceLand(resources)) {
            _nowPlayingBinding?.npArtistAlbum?.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            _nowPlayingBinding?.npSong?.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        }

        mMediaControlInterface.onGetMediaPlayerHolder()?.let { mph ->
            _npCoverBinding?.run {
                if (VersioningHelper.isMarshmallow()) {
                    setupNPCoverButtonsToasts(npPlaybackSpeed)
                    npPlaybackSpeed.setOnClickListener { view ->
                        DialogHelper.showPopupForPlaybackSpeed(requireActivity(), view)
                    }
                } else {
                    npPlaybackSpeed.visibility = View.GONE
                }

                npSaveTime.setOnClickListener { saveSongPosition() }
                npEqualizer.setOnClickListener { mUIControlInterface.onOpenEqualizer() }
                npLove.setOnClickListener {
                    ListsHelper.addToFavorites(
                        requireActivity(),
                        mph.currentSong,
                        canRemove = true,
                        0,
                        mph.launchedBy)
                    mUIControlInterface.onFavoritesUpdated(clear = false)
                    updateNpFavoritesIcon(requireActivity())
                }

                with(npRepeat) {
                    setImageResource(
                        ThemeHelper.getRepeatIcon(mph)
                    )
                    ThemeHelper.updateIconTint(
                        this,
                        if (mph.isRepeat1X || mph.isLooping || mph.isPauseOnEnd) {
                            ThemeHelper.resolveThemeAccent(requireActivity())
                        } else {
                            ContextCompat.getColor(requireActivity(), R.color.widgetsColor)
                        }
                    )
                    setOnClickListener { setRepeat() }
                    setupNPCoverButtonsToasts(npSaveTime, npLove, npEqualizer, this)
                }
            }
        }
    }

    private fun setupNPCoverButtonsToasts(vararg imageButtons: ImageButton) {
        val iterator = imageButtons.iterator()
        while (iterator.hasNext()) {
            iterator.next().setOnLongClickListener { btn ->
                btn.contentDescription.toString().toToast(requireActivity())
                return@setOnLongClickListener true
            }
        }
    }

    private fun setupPreciseVolumeHandler() {

        _npExtControlsBinding?.run {

            val isVolumeEnabled = goPreferences.isPreciseVolumeEnabled
            npVolumeValue.handleViewVisibility(show = isVolumeEnabled)
            npVolume.handleViewVisibility(show = isVolumeEnabled)
            npVolumeSeek.handleViewVisibility(show = isVolumeEnabled)

            if (isVolumeEnabled) {

                mMediaControlInterface.onGetMediaPlayerHolder()?.currentVolumeInPercent?.run {
                    npVolume.setImageResource(
                        ThemeHelper.getPreciseVolumeIcon(this)
                    )
                    npVolumeSeek.progress = this
                    npVolumeValue.text = this.toString()
                }

                npVolumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                    val defaultValueColor = _npExtControlsBinding?.npVolumeValue?.currentTextColor
                    val selectedColor = ThemeHelper.resolveThemeAccent(requireActivity())

                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            mMediaControlInterface.onGetMediaPlayerHolder()?.setPreciseVolume(progress)
                            npVolumeValue.text = progress.toString()
                            npVolume.setImageResource(
                                ThemeHelper.getPreciseVolumeIcon(progress)
                            )
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        npVolumeValue.setTextColor(selectedColor)
                        ThemeHelper.updateIconTint(
                            npVolume,
                            selectedColor
                        )
                    }
                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        npVolumeValue.setTextColor(defaultValueColor!!)
                        ThemeHelper.updateIconTint(
                            npVolume,
                            defaultValueColor
                        )
                    }
                })
            }
        }
    }

    private fun setupSeekBarProgressListener() {

        mMediaControlInterface.onGetMediaPlayerHolder()?.let { mph ->
            _nowPlayingBinding?.run {
                npSeekBar.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {

                        val defaultPositionColor = npSeek.currentTextColor
                        val selectedColor = ThemeHelper.resolveThemeAccent(requireActivity())
                        var userSelectedPosition = 0
                        var isUserSeeking = false

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
                                userSelectedPosition = progress
                            }
                            npSeek.text =
                                progress.toLong().toFormattedDuration(isAlbum = false, isSeekBar = true)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            isUserSeeking = true
                            npSeek.setTextColor(
                                selectedColor
                            )
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            if (isUserSeeking) {
                                npSeek.setTextColor(defaultPositionColor)
                                mph.onPauseSeekBarCallback()
                                isUserSeeking = false
                            }
                            if (mph.state != GoConstants.PLAYING) {
                                mMediaControlInterface.onUpdatePositionFromNP(userSelectedPosition)
                                npSeekBar.progress = userSelectedPosition
                            }
                            mph.seekTo(
                                userSelectedPosition,
                                updatePlaybackStatus = mph.isPlaying,
                                restoreProgressCallBack = !isUserSeeking
                            )
                        }
                    })
            }
        }
    }

    fun updateRepeatStatus(onPlaybackCompletion: Boolean) {
        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
            val resolvedIconsColor = ContextCompat.getColor(requireActivity(), R.color.widgetsColor)
            _npCoverBinding?.npRepeat?.setImageResource(
                ThemeHelper.getRepeatIcon(this)
            )
            when {
                onPlaybackCompletion -> ThemeHelper.updateIconTint(
                    _npCoverBinding?.npRepeat!!,
                    resolvedIconsColor
                )
                isRepeat1X or isLooping or isPauseOnEnd -> {
                    ThemeHelper.updateIconTint(
                        _npCoverBinding?.npRepeat!!, ThemeHelper.resolveThemeAccent(requireActivity())
                    )
                }
                else -> ThemeHelper.updateIconTint(
                    _npCoverBinding?.npRepeat!!,
                    resolvedIconsColor
                )
            }
        }
    }

    private fun loadNpCover(selectedSong: Music) {
        if (goPreferences.isCovers) {
            mAlbumIdNp = selectedSong.albumId
            _npCoverBinding?.npCover?.load(selectedSong.albumId?.toAlbumArtURI()) {
                error(ContextCompat.getDrawable(requireActivity(), R.drawable.album_art))
            }
        }
    }

    private fun setRepeat() {
        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
            repeat(updatePlaybackStatus = isPlaying)
            updateRepeatStatus(onPlaybackCompletion = false)
        }
    }

    private fun saveSongPosition() {
        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
            val song = currentSong
            when (val position = playerPosition) {
                0 -> _npCoverBinding?.npLove?.callOnClick()
                else -> {
                    ListsHelper.addToFavorites(requireActivity(), song, canRemove = false, position, launchedBy)
                    mUIControlInterface.onFavoriteAddedOrRemoved()
                }
            }
        }
    }

    private fun skip(isNext: Boolean) {
        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
            if (!isPlay) { isPlay = true }
            if (isSongFromPrefs) { isSongFromPrefs = false }
            if (isNext) {
                skip(isNext = true)
            } else {
                instantReset()
            }
        }
    }

    fun updateNpFavoritesIcon(context: Context) {
        val mediaPlayerHolder = mMediaControlInterface.onGetMediaPlayerHolder()
        _npCoverBinding?.run {
                mediaPlayerHolder?.currentSong?.let { song ->
                    val favorites = goPreferences.favorites
                    val isFavorite = favorites != null && favorites.contains(song.toSavedMusic(0, mediaPlayerHolder.launchedBy))
                    val favoritesButtonColor = if (isFavorite) {
                        npLove.setImageResource(R.drawable.ic_favorite)
                        ThemeHelper.resolveThemeAccent(context)
                    } else {
                        npLove.setImageResource(R.drawable.ic_favorite_empty)
                        ThemeHelper.resolveColorAttr(
                            context,
                            android.R.attr.colorButtonNormal
                        )
                    }
                    ThemeHelper.updateIconTint(npLove, favoritesButtonColor)
                }
        }
    }

    fun updateNpInfo() {
        mMediaControlInterface.onGetMediaPlayerHolder()?.currentSong?.let { song ->
            val selectedSongDuration = song.duration
            if (mAlbumIdNp != song.albumId && goPreferences.isCovers) {
                loadNpCover(song)
            }

            _nowPlayingBinding?.npSong?.text = song.title
            _nowPlayingBinding?.npArtistAlbum?.text =
                getString(
                    R.string.artist_and_album,
                    song.artist,
                    song.album
                )

            _nowPlayingBinding?.npDuration?.text =
                selectedSongDuration.toFormattedDuration(isAlbum = false, isSeekBar = true)
            _nowPlayingBinding?.npSeekBar?.max = song.duration.toInt()

            song.id?.toContentUri()?.toBitrate(requireActivity())?.let { (first, second) ->
                _nowPlayingBinding?.npRates?.text =
                    getString(R.string.rates, first, second)
            }
        }
        updateNpFavoritesIcon(requireActivity())
        updatePlayingStatus()
    }

    fun updateProgress(position: Int) {
        _nowPlayingBinding?.npSeekBar?.progress = position
    }

    fun updatePlayingStatus() {
        mMediaControlInterface.onGetMediaPlayerHolder()?.run {
            val isPlaying = state != GoConstants.PAUSED
            val drawable =
                if (isPlaying) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
            _npControlsBinding?.npPlay?.setImageResource(drawable)
        }
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
