package com.example.android.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.example.android.spotifystreamer.R;
import com.example.android.spotifystreamer.SpotifyTrack;
import com.example.android.spotifystreamer.Utils;
import com.example.android.spotifystreamer.action.MusicPlayer;
import com.example.android.spotifystreamer.action.MusicStatus;
import com.example.android.spotifystreamer.player.PlayerActivity;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by achiang on 8/26/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {

    private static final String LOG_TAG = MusicService.class.getSimpleName();
    private static final String MEDIA_SESSION_TAG = "MEDIA_SESSION_TAG";
    private static final int INTENT_GET_INT_EXTRA_DEFAULT = Integer.MIN_VALUE;
    private static final int FIRST_TRACK_ID = 0;
    private static final int NOTIFY_ID = 1;
    private static final float MIN_VOLUME = 0.0F;
    private static final float MAX_VOLUME = 1.0F;

    private int mCurrentTrackId;
    private String mCurrentTrackPreivewUrl;
    private SpotifyTrack mCurrentTrack;
    private List<SpotifyTrack> mTracks;
    private ScheduledExecutorService schedule;
    private WifiManager.WifiLock mWifiLock;
    private Handler mUiHandler;
    private MusicPlayerReceiver mMusicPlayerReceiver;
    private Bitmap mNotificationArtwork;
    private MediaPlayer mMediaPlayer;
    private MediaSession mMediaSession;
    private MediaController mMediaController;
    private PlaybackState mPlaybackState;
    private boolean mIsNewTrack;
    private boolean mIsConfigurationChange;

    /**
     * Binder is an interface between service and other components.
     */
    private final IBinder mMusicBinder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initMediaPlayer();
        initMediaSession();
        initBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get all track records from database.
        mTracks = Utils.getTracksFromDb(getApplicationContext());

        if (intent != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMusicBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releaseMediaPlayer();
        releaseMediaSession();
        releaseBroadcastReceiver();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // Reset the MediaPlayer to idle state from Error state.
        mp.reset();
        // Reset the playback state.
        setPlaybackStateToMediaSession(PlaybackState.STATE_NONE, PlaybackState.PLAYBACK_POSITION_UNKNOWN, MIN_VOLUME);

        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            // Tell the player dialog fragment to response when the music is playing.
            sendMsgToUiHandler(MusicStatus.PLAYING);
            // Update the playback state to Playing.
            setPlaybackStateToMediaSession(PlaybackState.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), MAX_VOLUME);
            // Stop updating the progress of playback.
            stopCheckingPlaybackProgress();
            // Start updating the position of playback to the player dialog fragment.
            startCheckingPlaybackProgress();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Release the wifi lock when streaming music is done.
        releaseWifiLock();
        // Update the playback state to Stopped.
        setPlaybackStateToMediaSession(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, MIN_VOLUME);
        // Tell the player dialog fragment that the music is stopped.
        sendMsgToUiHandler(MusicStatus.DONE);
        // Stop updating the progress of playback.
        stopCheckingPlaybackProgress();

        doStopForeground();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // start playing music when the buffering is done.
        mp.start();
        // Tell the player dialog fragment that the music is playing now.
        sendMsgToUiHandler(MusicStatus.PLAYING);
        // Update the playback state to Playing.
        setPlaybackStateToMediaSession(PlaybackState.STATE_PLAYING, mp.getCurrentPosition(), MAX_VOLUME);
        // Stop updating the progress of playback.
        stopCheckingPlaybackProgress();
        // Start updating the progress of playback.
        startCheckingPlaybackProgress();

        doStartForeground();
    }

    private void startPrevPlayback() {
        // Set the current track id to the previous one, but not exceed the first track which is 0.
        mCurrentTrackId = (mCurrentTrackId > 0 ? --mCurrentTrackId : FIRST_TRACK_ID);
        // Tell the player dialog fragment to update its ui to the previous track.
        sendMsgToUiHandler(MusicStatus.UPDATE_TRACK_UI_SKIP_TO_PREV);
        // Start playing the music based on the current track id.
        startPlayback(mCurrentTrackId);
    }

    private void startNextPlayback() {
        final int lastTrackId = mTracks.size() - 1;
        // Set the current track id to the next one, but not exceed the last track.
        mCurrentTrackId = (mCurrentTrackId < lastTrackId ? ++mCurrentTrackId : lastTrackId);
        // Tell the player dialog fragment to update its ui to the next track.
        sendMsgToUiHandler(MusicStatus.UPDATE_TRACK_UI_SKIP_TO_NEXT);
        // Start playing the music based on the current track id.
        startPlayback(mCurrentTrackId);
    }

    /**
     * Update the current track based on track id passed in by intent.
     * This will be called when an action MusicPlayer.PLAY is invoked.
     *
     * @param intent: contains selected track id.
     */
    private void setCurrentTrack(Intent intent) {
        mIsNewTrack = false;
        mIsConfigurationChange = false;

        if (intent != null) {
            if (intent.hasExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS))) {
                int trackId = intent.getIntExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS), FIRST_TRACK_ID);
                SpotifyTrack track = mTracks.get(trackId);
                String previewUrl = track.getPreview_url();

                if (!previewUrl.equals(mCurrentTrackPreivewUrl)) {
                    mCurrentTrackId = trackId;
                    mCurrentTrack = track;
                    mIsNewTrack = true;
                }

                mIsConfigurationChange = true;
            }
        }
    }

    private void startPlayback(int trackId) {
        SpotifyTrack track = mTracks.get(trackId);

        // Sets the reference to the new track.
        mCurrentTrackPreivewUrl = track.getPreview_url();
        mCurrentTrack = track;
        mCurrentTrackId = trackId;

        new LoadArtworkTask().execute();

        // The player should display a 'play' button before playing the song,
        // and display a 'pause' button when the song is playing.
        sendMsgToUiHandler(MusicStatus.PAUSED);

        // acquire wifi lock before streaming.
        acquireWifiLock();

        // Reset to the idle state.
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // Moves to the initialized state.
            mMediaPlayer.setDataSource(mCurrentTrack.getPreview_url());
            // Moves to the Prepared state.
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pausePlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();

            // Notification should display "play" button when the streaming is paused.
            showNotification(createNotification(MusicPlayer.PLAY));
            // Set the playback state to paused.
            setPlaybackStateToMediaSession(PlaybackState.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), MIN_VOLUME);
            // Tell the player that the streaming is pauses now.
            sendMsgToUiHandler(MusicStatus.PAUSED);
            // Stop updating the progress of playback.
            stopCheckingPlaybackProgress();
        }
    }

    /**
     * resumePlayback would be called after
     * case 1: pausePlayback() is called
     * case 2: re-play the song which is done streaming
     * <p/>
     * The wifi lock is released each time when the streaming is completed,
     * we need to re-acquire it to re-play the same song.
     */
    private void resumePlayback() {
        if (!isWifiLock()) {
            acquireWifiLock();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            // Set notification to display "pause" button.
            showNotification(createNotification(MusicPlayer.PAUSE));
            // Set the playback state to playing.
            setPlaybackStateToMediaSession(PlaybackState.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), MAX_VOLUME);
            // Tell the player that the music is playing now.
            sendMsgToUiHandler(MusicStatus.PLAYING);
            // Start updating the progress of the playback.
            startCheckingPlaybackProgress();
        }
    }

    /**
     * Get seekbar position passed in by intent from player dialog fragment.
     *
     * @param intent: contains the position of seekbar.
     * @return the position of seekbar.
     */
    private int getSeekToPos(Intent intent) {
        if (intent != null && intent.hasExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_SEEK_TO))) {
            int seekToInMilliseconds = intent.getIntExtra(
                    getString(R.string.INTENT_KEY_PLAYER_TRACK_SEEK_TO),
                    INTENT_GET_INT_EXTRA_DEFAULT);
            return seekToInMilliseconds;
        }

        return INTENT_GET_INT_EXTRA_DEFAULT;
    }

    /**
     * Determine if the music is playing.
     * This method will be called by PlayerDialogFragment.
     *
     * @return
     */
    public boolean musicIsPlaying() {
        return mMediaPlayer.isPlaying();
    }

    /**
     * References to the UI thread' handler for interacting with UI thread's message queue.
     *
     * @param handler: UI thread's handler
     */
    public void registerHandler(Handler handler) {

        if (mUiHandler != handler) {
            mUiHandler = handler;
        }
    }

    private void sendMsgToUiHandler(int action) {
        if (mUiHandler != null) {
            Message msg = mUiHandler.obtainMessage(action);
            mUiHandler.sendMessage(msg);
        }
    }

    private void sendMsgToUiHandler(int action, int progress, int duration) {
        if (mUiHandler != null) {
            Message msg = mUiHandler.obtainMessage(action, progress, duration);
            mUiHandler.sendMessage(msg);
        }
    }

    /**
     * Periodically update the progress of playback.
     */
    private void startCheckingPlaybackProgress() {
        schedule = Executors.newSingleThreadScheduledExecutor();

        Runnable checking = new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer.isPlaying()) {
                    int duration = mMediaPlayer.getDuration();
                    int currentPos = mMediaPlayer.getCurrentPosition();
                    sendMsgToUiHandler(MusicStatus.UPDATE_PROGRESS, currentPos, duration);
                }
            }
        };

        // Set the period to 100 millisecond, the seekbar can moving smoothly.
        schedule.scheduleAtFixedRate(checking, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop updating the progress of playback when the music is not playing.
     */
    private void stopCheckingPlaybackProgress() {
        if (schedule != null && !schedule.isShutdown()) {
            schedule.shutdownNow();
        }
    }

    /**
     * Create a notification which contains 'previous', 'play/pause', and 'next' button.
     *
     * @param playerAction: determines the middle button is 'play' or 'pause'.
     * @return Notification
     */
    private Notification createNotification(String playerAction) {
        // PendingIntent for back to PlayerActivity.
        Intent playerActivityIntent = new Intent(getApplicationContext(), PlayerActivity.class);
        Bundle args = new Bundle();
        args.putLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS), (long) mCurrentTrackId);
        playerActivityIntent.putExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS), args);
        PendingIntent playerActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, playerActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // PendingIntent for skip to previous track.
        Intent prevIntent = new Intent(getApplicationContext(), MusicService.class);
        prevIntent.setAction(MusicPlayer.PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, 0);

        // PendingIntent for play or pause the track based on the playerAction.
        Intent intent = new Intent(getApplicationContext(), MusicService.class);
        intent.setAction(playerAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);

        Notification.Action notificationAction;
        if (playerAction.equals(MusicPlayer.PLAY)) {
            notificationAction = new Notification.Action.Builder(R.drawable.ic_play_arrow_white_36dp, "Play", pendingIntent).build();
        } else {
            notificationAction = new Notification.Action.Builder(R.drawable.ic_pause_white_36dp, "Pause", pendingIntent).build();
        }

        // PendingIntent for skip to next track.
        Intent nextIntent = new Intent(getApplicationContext(), MusicService.class);
        nextIntent.setAction(MusicPlayer.NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, 0);

        Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
        mediaStyle.setMediaSession(mMediaSession.getSessionToken());
        mediaStyle.setShowActionsInCompactView(0, 1, 2);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_audiotrack_white_36dp)
                .setLargeIcon(mNotificationArtwork)
                .setContentTitle(mCurrentTrack.getTrack_name())
                .setContentText(mCurrentTrack.getArtist_name())
                .setSubText(mCurrentTrack.getAlbum_name())
                .setStyle(mediaStyle)
                .setContentIntent(playerActivityPendingIntent)
                .addAction(new Notification.Action.Builder(R.drawable.ic_skip_previous_white_36dp, "Previous", prevPendingIntent).build())
                .addAction(notificationAction)
                .addAction(new Notification.Action.Builder(R.drawable.ic_skip_next_white_36dp, "Next", nextPendingIntent).build())
                .build();

        return notification;
    }

    private void showNotification(Notification notification) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFY_ID, notification);
    }

    private void cancelNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFY_ID);
    }

    private void doStartForeground() {
        startForeground(NOTIFY_ID, createNotification(MusicPlayer.PAUSE));
    }

    private void doStopForeground() {
        stopForeground(true);
    }


    private class MusicPlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MusicPlayer.PLAY:
                    setCurrentTrack(intent);
                    mMediaController.getTransportControls().play();
                    break;

                case MusicPlayer.PREV:
                    mMediaController.getTransportControls().skipToPrevious();
                    break;

                case MusicPlayer.NEXT:
                    mMediaController.getTransportControls().skipToNext();
                    break;

                case MusicPlayer.PAUSE:
                    mMediaController.getTransportControls().pause();
                    break;

                case MusicPlayer.SEEK_TO:
                    long pos = (long) getSeekToPos(intent);
                    mMediaController.getTransportControls().seekTo(pos);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This task contains two jobs.
     * 1. Load image in the format of Bitmap and set mNotificationArtwork refers to it.
     * 2. Publish a notification when the first job is done.
     */
    private class LoadArtworkTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                return Picasso.with(getApplicationContext())
                        .load(mCurrentTrack.getSmall_image_url())
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                mNotificationArtwork = result;
                showNotification(createNotification(MusicPlayer.PLAY));
            }
        }
    }

    private void setPlaybackStateToMediaSession(int state, long position, float playbackSpeed) {
        mPlaybackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY |
                        PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackState.ACTION_SKIP_TO_NEXT)
                .setState(state, position, playbackSpeed).build();
        mMediaSession.setPlaybackState(mPlaybackState);
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            super.onPlay();

            if (mIsNewTrack) {
                startPlayback(mCurrentTrackId);
                return;
            }

            int state = mPlaybackState.getState();

            switch (state) {
                case PlaybackState.STATE_NONE: // initial state.
                    startPlayback(mCurrentTrackId);
                    break;

                case PlaybackState.STATE_PLAYING: // laid-back when the music is currently streaming.
                    sendMsgToUiHandler(MusicStatus.DISMISS);
                    break;

                case PlaybackState.STATE_STOPPED: // streaming is completed.
                    if (!mIsConfigurationChange) {
                        startPlayback(mCurrentTrackId);
                    } else {
                        sendMsgToUiHandler(MusicStatus.DISMISS);
                    }
                    break;

                case PlaybackState.STATE_PAUSED: // resume the playback when the streaming is paused.
                    if (!mIsConfigurationChange) {
                        resumePlayback();
                    }
                    sendMsgToUiHandler(MusicStatus.DISMISS);
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
                pausePlayback();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            startPrevPlayback();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            startNextPlayback();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            mMediaPlayer.seekTo((int) pos);
        }
    }

    /**
     * Initialize resources.
     */

    private void initMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new MediaPlayer();

        // Wake lock is used to prevent the system from interfering with playback.
        // It tells the system that this app is using some features that should stay available
        // even if the phone is idle.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        // Register callbacks.
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
    }

    private void initMediaSession() {
        if (mMediaSession != null) {
            return;
        }

        mMediaSession = new MediaSession(getApplicationContext(), MEDIA_SESSION_TAG);
        mMediaController = new MediaController(getApplicationContext(), mMediaSession.getSessionToken());

        // Indicate the music is not playing yet.
        setPlaybackStateToMediaSession(
                PlaybackState.STATE_NONE,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                MIN_VOLUME);

        // Receive updates for the MediaSession, such as media buttons, transport controls.
        mMediaSession.setCallback(new MediaSessionCallback());
        // Indicate that this MediaSession can handle media buttons and transport controls.
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setActive(true);
    }

    private void initBroadcastReceiver() {
        if (mMusicPlayerReceiver != null) {
            return;
        }

        mMusicPlayerReceiver = new MusicPlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlayer.PLAY);
        filter.addAction(MusicPlayer.PAUSE);
        filter.addAction(MusicPlayer.PREV);
        filter.addAction(MusicPlayer.NEXT);
        filter.addAction(MusicPlayer.SEEK_TO);

        // Dynamically register these actions.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMusicPlayerReceiver, filter);
    }

    /**
     * Release all allocated resources.
     */

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void releaseMediaSession() {
        if (mMediaSession != null) {
            mMediaSession.setCallback(null);
            mMediaSession.release();
            mMediaSession = null;
        }
    }

    private void releaseBroadcastReceiver() {
        if (mMusicPlayerReceiver != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMusicPlayerReceiver);
            mMusicPlayerReceiver = null;
        }
    }

    /**
     * We should acquire the 'Wifi Lock' to streaming music,
     * and release it when we no longer need it.
     * 'WAKE_LOCK' is also required to be declared in the manifest file.
     */
    private void acquireWifiLock() {
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "THE_WIFI_LOCK");
        mWifiLock.acquire();
    }

    /**
     * Release wifi lock.
     */
    private void releaseWifiLock() {
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
            mWifiLock = null;
        }
    }

    /**
     * Determine the wifi lock state.
     *
     * @return true: wifi lock is acquired; false otherwise.
     */
    private boolean isWifiLock() {
        if (mWifiLock == null || !mWifiLock.isHeld()) {
            return false;
        }

        return true;
    }
}
