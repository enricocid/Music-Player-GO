package com.iven.musicplayergo.ui

import com.iven.musicplayergo.music.Music

interface UIControlInterface {
    fun onArtistSelected(artist: String)
    fun onSongSelected(song: Music, album: List<Music>)
    fun onShuffleSongs()
    //  fun onSongSelected(song: Music, currentSongAlbum: List<Music>)
    fun onVisibleItemsUpdated()
}
