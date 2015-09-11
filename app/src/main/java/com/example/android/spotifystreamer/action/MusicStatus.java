package com.example.android.spotifystreamer.action;

/**
 * Created by achiang on 9/5/15.
 */
public final class MusicStatus {
    public static final int PLAYING = 1;
    public static final int PAUSED = PLAYING << 1;
    public static final int STOPPED = PLAYING << 2;
    public static final int DONE = PLAYING << 3;
    public static final int UPDATE_PROGRESS = PLAYING << 4;
    public static final int DISMISS = PLAYING << 5;
    public static final int UPDATE_TRACK_UI_SKIP_TO_PREV = PLAYING << 6;
    public static final int UPDATE_TRACK_UI_SKIP_TO_NEXT = PLAYING << 7;
}
