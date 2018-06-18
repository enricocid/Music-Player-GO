/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iven.musicplayergo.playback;

import android.app.Activity;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;

import com.iven.musicplayergo.MainActivity;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Song;

import java.util.List;

/**
 * Allows {@link MainActivity} to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerAdapter {

    void initMediaPlayer();

    void release();

    boolean isMediaPlayer();

    boolean isPlaying();

    void resumeOrPause();

    void reset();

    boolean isReset();

    void instantReset();

    void skip(final boolean isNext);

    void openEqualizer(@NonNull final Activity activity);

    void seekTo(final int position);

    void setPlaybackInfoListener(final PlaybackInfoListener playbackInfoListener);

    Song getCurrentSong();

    @PlaybackInfoListener.State
    int getState();

    int getPlayerPosition();

    void registerNotificationActionsReceiver(final boolean isRegister);

    Album getSelectedAlbum();

    void setSelectedAlbum(final Album album);

    void setCurrentSong(@NonNull final Song song, @NonNull final List<Song> songs);

    MediaPlayer getMediaPlayer();

    void onPauseActivity();

    void onResumeActivity();
}
