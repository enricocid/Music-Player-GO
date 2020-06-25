package com.iven.musicplayergo.viewpager

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentAllMusicBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.toFormattedDuration
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.ui.SongsViewHolder

/**
 * A simple [Fragment] subclass.
 * Use the [AllMusicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllMusicFragment : Fragment(R.layout.fragment_all_music), SearchView.OnQueryTextListener {

    private lateinit var mAllMusicFragmentBinding: FragmentAllMusicBinding

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()
    private var mAllMusic: MutableList<Music>? = null
    private val mDataSource = emptyDataSource()

    private lateinit var mUIControlInterface: UIControlInterface

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAllMusicFragmentBinding = FragmentAllMusicBinding.bind(view)

        val musicRepository = MusicRepository.getInstance()
        mAllMusic = musicRepository.deviceMusicFiltered

        setMusicDataSource(mAllMusic)

        mAllMusicFragmentBinding.allMusicRv.apply {

            // setup{} is an extension method on RecyclerView
            setup {

                // item is a `val` in `this` here
                withDataSource(mDataSource)

                if (!ThemeHelper.isDeviceLand(resources)) {
                    ThemeHelper.getRecyclerViewDivider(requireContext())
                }

                withItem<Music, SongsViewHolder>(R.layout.music_item) {
                    onBind(::SongsViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = item.title
                        duration.text = item.duration.toFormattedDuration(
                            isAlbum = false,
                            isSeekBar = false
                        )
                        subtitle.text =
                            getString(R.string.artist_and_album, item.artist, item.album)
                    }

                    onClick {
                        mMediaPlayerHolder.mediaPlayerInterface?.onSongSelected(
                            item,
                            MusicOrgHelper.getAlbumSongs(
                                item.artist,
                                item.album
                            ),
                            LaunchedBy.ArtistView
                        )
                    }

                    onLongClick { index ->
                        DialogHelper.showDoSomethingPopup(
                            requireContext(),
                            findViewHolderForAdapterPosition(index)?.itemView,
                            item,
                            LaunchedBy.ArtistView
                        )
                    }
                }
            }
        }

        mAllMusicFragmentBinding.searchToolbar.apply {

            inflateMenu(R.menu.menu_all_music)

            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity(mMediaPlayerHolder.state == GoConstants.PLAYING || mMediaPlayerHolder.state == GoConstants.RESUMED)
            }

            menu.apply {
                findItem(R.id.action_shuffle_am).setOnMenuItemClickListener {
                    mMediaPlayerHolder.mediaPlayerInterface?.onShuffleSongs(
                        mAllMusic,
                        LaunchedBy.ArtistView
                    )
                    return@setOnMenuItemClickListener true
                }
                val searchView = findItem(R.id.action_search).actionView as SearchView
                searchView.apply {
                    setOnQueryTextListener(this@AllMusicFragment)
                    setOnQueryTextFocusChangeListener { _, hasFocus ->
                        menu.findItem(R.id.action_shuffle_am).isVisible = !hasFocus
                    }
                }
            }
        }
    }

    private fun setMusicDataSource(musicList: List<Music>?) {
        musicList?.apply {
            mDataSource.set(this)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        setMusicDataSource(ListsHelper.processQueryForMusic(newText, mAllMusic) ?: mAllMusic)
        return false
    }

    override fun onQueryTextSubmit(query: String?) = false

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AllMusicFragment.
         */
        @JvmStatic
        fun newInstance() = AllMusicFragment()
    }
}
