package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.DataSource
import com.afollestad.recyclical.datasource.dataSourceOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.iven.musicplayergo.R
import com.iven.musicplayergo.music.Album
import com.iven.musicplayergo.music.Music
import com.iven.musicplayergo.music.MusicUtils
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.ui.AlbumsViewHolder
import com.iven.musicplayergo.ui.GenericViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.android.synthetic.main.fragment_artist_details.*

/**
 * A simple [Fragment] subclass.
 * Use the [ArtistDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArtistDetailsFragment : Fragment() {

    private lateinit var mSelectedAlbumsDataSource: DataSource<Any>
    private lateinit var mAlbumSongsDataSource: DataSource<Any>

    private lateinit var mAlbumsRecyclerView: RecyclerView
    private lateinit var mSongsRecyclerView: RecyclerView

    private lateinit var mSelectedArtistAlbums: List<Album>
    private var mSelectedArtist = "unknown"
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mSelectedAlbum: Album

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG)?.let {
            Log.d(it, it)
            mSelectedArtist = it
            mSelectedArtistAlbums = musicLibrary.allAlbumsForArtist[mSelectedArtist]!!
            mSelectedAlbum = mSelectedArtistAlbums[0]
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement MyInterface ")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_artist_details, container, false)
    }

    private fun swapAlbum(songs: MutableList<Music>) {
        mSelectedAlbumsDataSource.set(mSelectedArtistAlbums)
        mAlbumSongsDataSource.set(songs)
        mSongsRecyclerView.scrollToPosition(0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context != null) {

            mAlbumsRecyclerView = albums_rv
            mSongsRecyclerView = songs_rv

            selected_discography_artist.text = mSelectedArtist

            selected_artist_album_count.text = getString(
                R.string.artist_info,
                mSelectedArtistAlbums.size,
                musicLibrary.allSongsForArtist.getValue(mSelectedArtist).size
            )

            selected_disc.text = mSelectedAlbum.title
            selected_disc_year.text = mSelectedAlbum.year

            /*       shuffle_button.setOnClickListener {
                       if (::mMediaPlayerHolder.isInitialized) {
                           if (!mSeekBar.isEnabled) mSeekBar.isEnabled = true
                           val albumSongs = mSelectedAlbum?.music
                           albumSongs?.shuffle()
                           startPlayback(albumSongs!![0], albumSongs)
                       }
                   }*/

            close_button.setOnClickListener { activity?.onBackPressed() }

            mSelectedAlbumsDataSource = dataSourceOf(mSelectedArtistAlbums)

            mAlbumsRecyclerView.setup {
                withDataSource(mSelectedAlbumsDataSource)
                withLayoutManager(
                    LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                )

                withItem<Album, AlbumsViewHolder>(R.layout.album_item) {
                    onBind(::AlbumsViewHolder) { _, item ->
                        // AlbumsViewHolder is `this` here
                        album.text = item.title
                        year.text = item.year

                        checkbox.visibility =
                            if (mSelectedAlbum.title != item.title) View.GONE else View.VISIBLE
                    }

                    onClick {

                        if (mSelectedAlbum.title != item.title) {

                            mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumFromList(
                                    item.title,
                                    mSelectedArtistAlbums
                                ).second
                            )

                            mAlbumsRecyclerView.adapter?.notifyItemChanged(
                                MusicUtils.getAlbumFromList(
                                    mSelectedAlbum.title,
                                    mSelectedArtistAlbums
                                ).second
                            )
                            mSelectedAlbum = item
                            swapAlbum(item.music!!)
                        }
                    }
                }
            }

            mAlbumSongsDataSource = dataSourceOf(mSelectedAlbum.music!!)

            // setup{} is an extension method on RecyclerView
            mSongsRecyclerView.setup {
                // item is a `val` in `this` here
                withDataSource(mAlbumSongsDataSource)
                withItem<Music, GenericViewHolder>(R.layout.generic_item) {
                    onBind(::GenericViewHolder) { _, item ->
                        // GenericViewHolder is `this` here
                        title.text = getString(
                            R.string.track_song,
                            MusicUtils.formatSongTrack(item.track),
                            item.title
                        )
                        subtitle.text = MusicUtils.formatSongDuration(item.duration)
                    }

                    onClick {
                        mUIControlInterface.onSongSelected(
                            item,
                            MusicUtils.getAlbumFromList(
                                item.album,
                                mSelectedArtistAlbums
                            ).first.music!!.toList()
                        )
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MusicFragment.
         */
        @JvmStatic
        fun newInstance(selectedArtist: String) = ArtistDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(TAG, selectedArtist)
            }
        }

        @JvmStatic
        val TAG = "SELECTED_ARTIST"
    }
}
