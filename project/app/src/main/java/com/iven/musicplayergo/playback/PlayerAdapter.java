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
import android.support.v4.media.session.PlaybackStateCompat;

import com.iven.musicplayergo.MainActivity;
import com.iven.musicplayergo.models.Song;

import java.util.List;

/**
 * Allows {@link MainActivity} to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerAdapter {

    void play();

    void release();

    boolean isMediaPlayer();

    boolean isPlaying();

    void resumeOrPause();

    void reset();

    boolean isReset();

    void instantReset();

    void skip(boolean isNext);

    void openEqualizer(Activity activity);

    void seekTo(int position);

    void setPlaybackInfoListener(PlaybackInfoListener playbackInfoListener);

    Song getCurrentSong();

    @PlaybackStateCompat.State
    int getState();

    int getPlayerPosition();

    void registerNotificationActionsReceiver(boolean isRegister);

    void setCurrentSong(Song song, List<Song> songs);
}
