package com.iven.musicplayergo.ui

import com.iven.musicplayergo.models.Music
import com.iven.musicplayergo.player.MediaPlayerHolder

interface MediaControlInterface {
    fun onSongSelected(song: Music?, songs: List<Music>?, songLaunchedBy: String)
    fun onSongsShuffled(
        songs: List<Music>?,
        songLaunchedBy: String
    )
    fun onAddToQueue(song: Music?)
    fun onAddAlbumToQueue(
        songs: List<Music>?,
        // first: force play, second: restore song
        forcePlay: Pair<Boolean, Music?>
    )
    fun onUpdatePlayingAlbumSongs(songs: List<Music>?)
    fun onPlaybackSpeedToggled()
    fun onHandleCoverOptionsUpdate()
    fun onUpdatePositionFromNP(position: Int)
}
