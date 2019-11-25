package com.iven.musicplayergo.ui

import com.iven.musicplayergo.music.Music

interface UIControlInterface {
    fun onArtistSelected(artist: String)
    fun onSongSelected(song: Music, songs: List<Music>)
    fun onShuffleSongs(songs: MutableList<Music>)
    fun onLovedSongsUpdate()
}
