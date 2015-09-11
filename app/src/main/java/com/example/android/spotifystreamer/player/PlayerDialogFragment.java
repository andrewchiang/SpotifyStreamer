package com.example.android.spotifystreamer.player;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.spotifystreamer.R;
import com.example.android.spotifystreamer.SpotifyTrack;
import com.example.android.spotifystreamer.Utils;
import com.example.android.spotifystreamer.action.MusicPlayer;
import com.example.android.spotifystreamer.action.MusicStatus;
import com.example.android.spotifystreamer.service.MusicService;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by achiang on 8/24/15.
 */
public class PlayerDialogFragment extends DialogFragment
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String LOG_TAG = PlayerDialogFragment.class.getSimpleName();
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int MILLISECONDS_PER_MINUTE = SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;
    private static final int TEMP_MAX_DURATION = 30000; // in milliseconds.
    private TextView mArtistTextView;
    private TextView mAlbumTextView;
    private TextView mTrackTextView;
    private ImageView mArtworkImageView;
    private SeekBar mSeekBar;
    private TextView mStartTextView;
    private TextView mEndTextView;
    private ImageButton mPreviousImageButton;
    private ImageButton mPlayPauseIamgeButton;
    private ImageButton mNextImageButton;
    private boolean mIsPlaying;
    private int mElapsedTime;
    private int mDuration;
    private int mCurrentTrackId;
    private SpotifyTrack mCurrentTrack;
    private List<SpotifyTrack> mTracksInDB;
    private MusicService mMusicService;
    private boolean mBound;
    private UiHandler mUiHandler = new UiHandler(this);
    private ProgressDialog mBufferingProgressDialog;

    /**
     * To solve "The Handler should be static or leaks might occur" problem.
     * The solution is shown bellow:
     * Romain Guy -> https://groups.google.com/forum/?fromgroups=#%21msg/android-developers/1aPZXZG6kWk/lIYDavGYn5UJ
     * Alex Lockwood -> http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
     */
    private static class UiHandler extends Handler {
        private final WeakReference<PlayerDialogFragment> playerDialogFragmentWeakReference;

        public UiHandler(PlayerDialogFragment playerDialogFragment) {
            playerDialogFragmentWeakReference = new WeakReference<PlayerDialogFragment>(playerDialogFragment);
        }

        /**
         * Handle messages sent from MusicService to update the UI components.
         *
         * @param msg: contain the action defined in MusicStatus
         */
        @Override
        public void handleMessage(Message msg) {
            PlayerDialogFragment dialogFragment = playerDialogFragmentWeakReference.get();

            if (dialogFragment != null) {
                switch (msg.what) {
                    case MusicStatus.PLAYING:
                        dialogFragment.updatePlayPauseImageButton(true);
                        // Dismiss the progress dialog when the song is playing.
                        dialogFragment.dismissBufferingProgressDialog();
                        break;

                    case MusicStatus.PAUSED:
                    case MusicStatus.STOPPED:
                    case MusicStatus.DONE:
                        dialogFragment.updatePlayPauseImageButton(false);
                        break;

                    case MusicStatus.UPDATE_PROGRESS:
                        int currentPos = msg.arg1;
                        int duration = msg.arg2;
                        dialogFragment.updateTrackProgress(dialogFragment.mSeekBar, currentPos, duration,
                                dialogFragment.mStartTextView, dialogFragment.mEndTextView);
                        break;
                    case MusicStatus.DISMISS:
                        dialogFragment.dismissBufferingProgressDialog();
                        break;

                    case MusicStatus.UPDATE_TRACK_UI_SKIP_TO_PREV:
                        dialogFragment.syncTrackUiWithNotification(MusicStatus.UPDATE_TRACK_UI_SKIP_TO_PREV);
                        break;
                    case MusicStatus.UPDATE_TRACK_UI_SKIP_TO_NEXT:
                        dialogFragment.syncTrackUiWithNotification(MusicStatus.UPDATE_TRACK_UI_SKIP_TO_NEXT);
                        break;
                    default:
                        Log.d(LOG_TAG, "Invalid action");
                }
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            mMusicService = ((MusicService.MusicBinder) service).getService();
            mMusicService.registerHandler(mUiHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mUiHandler = null;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the existing top tracks info for restoring later.
        outState.putParcelableArrayList(getString(R.string.PARCEL_KEY_TOP_TRACKS_RESULT),
                (ArrayList<SpotifyTrack>) mTracksInDB);

        // Save the current track id.
        outState.putInt(getString(R.string.PARCEL_KEY_CURRENT_TRACK_ID), mCurrentTrackId);

        // Save the state of playing music.
        outState.putBoolean(getString(R.string.PARCEL_KEY_MUSIC_IS_PLAYING), mIsPlaying);

        // Save the state of the current position of seekbar.
        outState.putInt(getString(R.string.PARCEL_KEY_SEEKBAR_CURRENT_POS), mSeekBar.getProgress());

        // Save the state of the max of seekbar.
        outState.putInt(getString(R.string.PARCEL_KEY_SEEKBAR_MAX), mSeekBar.getMax());
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(getActivity(), MusicService.class);

        serviceIntent.putExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS), mCurrentTrackId);
        serviceIntent.setAction(MusicPlayer.PLAY);

        getActivity().startService(serviceIntent);
        getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mBound) {
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pressing the volume keys on the device affect the audio stream you specify.
        // see "https://developer.android.com/training/managing-audio/volume-playback.html#IdentifyStream".
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.track_player, container, false);

        mPreviousImageButton = (ImageButton) rootView.findViewById(R.id.previous_imagebutton);
        mPreviousImageButton.setOnClickListener(this);

        mNextImageButton = (ImageButton) rootView.findViewById(R.id.next_imagebutton);
        mNextImageButton.setOnClickListener(this);

        mPlayPauseIamgeButton = (ImageButton) rootView.findViewById(R.id.play_pause_imagebutton);
        mPlayPauseIamgeButton.setOnClickListener(this);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.duration_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mStartTextView = (TextView) rootView.findViewById(R.id.duration_start_textview);
        mEndTextView = (TextView) rootView.findViewById(R.id.duration_end_textview);

        if (savedInstanceState == null) {
            // Retrieve all tracks from the database for the first time launching this player.
            mTracksInDB = Utils.getTracksFromDb(getActivity());

            // Get the selected track id.
            mCurrentTrackId = (int) getArguments().getLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS));

            // Set the current position and max duration to default.
            mElapsedTime = 0;
            mDuration = TEMP_MAX_DURATION;
        } else {
            // Restore top tracks info.
            mTracksInDB = savedInstanceState
                    .getParcelableArrayList(getString(R.string.PARCEL_KEY_TOP_TRACKS_RESULT));

            // Restore the current track id.
            mCurrentTrackId = savedInstanceState
                    .getInt(getString(R.string.PARCEL_KEY_CURRENT_TRACK_ID));

            // Restore the state of playing music.
            mIsPlaying = savedInstanceState
                    .getBoolean(getString(R.string.PARCEL_KEY_MUSIC_IS_PLAYING));

            // Restore the current position of the track.
            mElapsedTime = savedInstanceState
                    .getInt(getString(R.string.PARCEL_KEY_SEEKBAR_CURRENT_POS));
            // Restore the duration of the track.
            mDuration = savedInstanceState
                    .getInt(getString(R.string.PARCEL_KEY_SEEKBAR_MAX));
        }

        updatePlayPauseImageButton(mIsPlaying);

        // Set the current track based on the current track id.
        mCurrentTrack = mTracksInDB.get(mCurrentTrackId);

        // Update the track info based on the current track.
        updateTrackUI(rootView, mCurrentTrack);

        // Update the progress of the SeekBar.
        updateTrackProgress(mSeekBar, mElapsedTime, mDuration, mStartTextView, mEndTextView);

        return rootView;
    }

    /**
     * Response to clicking button.
     *
     * @param v: ImageButton
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.previous_imagebutton:
                // Broadcast "Previous track" action.
                sendBroadcast(MusicPlayer.PREV);
                updateTrackProgress(mSeekBar, 0, TEMP_MAX_DURATION, mStartTextView, mEndTextView);
                break;

            case R.id.play_pause_imagebutton:

                if (mMusicService.musicIsPlaying()) {
                    // Because the music is playing, broadcast "Pause track" action.
                    sendBroadcast(MusicPlayer.PAUSE);
                } else {
                    // Because the music is paused, broadcast "Resume track" action.
                    sendBroadcast(MusicPlayer.PLAY);
                }

                break;

            case R.id.next_imagebutton:
                // Broadcast "Next track" action.
                sendBroadcast(MusicPlayer.NEXT);
                // Reset the seek bar status.
                updateTrackProgress(mSeekBar, 0, TEMP_MAX_DURATION, mStartTextView, mEndTextView);
                break;

            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Do nothing.
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing.
    }

    /**
     * Broadcast the "Seek To" action when the user stops moving the seek bar.
     * @param seekBar
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        sendBroadcast(MusicPlayer.SEEK_TO);
    }

    /**
     * Update the player user interface information based on the current track.
     * @param rootView
     * @param track
     */
    private void updateTrackUI(View rootView, SpotifyTrack track) {
        if (mCurrentTrack != track) {
            mCurrentTrack = track;
        }

        mArtistTextView = (TextView) rootView.findViewById(R.id.artist_name_textview);
        mArtistTextView.setText(track.getArtist_name());

        mAlbumTextView = (TextView) rootView.findViewById(R.id.album_name_textview);
        mAlbumTextView.setText(track.getAlbum_name());

        mTrackTextView = (TextView) rootView.findViewById(R.id.track_name_textview);
        mTrackTextView.setText(track.getTrack_name());

        // Get the larger artwork if available.
        String artwork;
        if (!track.getLarge_image_url().isEmpty()) {
            artwork = track.getLarge_image_url();
        } else {
            artwork = track.getSmall_image_url();
        }

        mArtworkImageView = (ImageView) rootView.findViewById(R.id.album_artwork_imageview);
        if (!artwork.isEmpty()) {
            Picasso.with(getActivity())
                    .load(artwork)
                    .resizeDimen(R.dimen.player_artwork, R.dimen.player_artwork)
                    .centerCrop()
                    .into(mArtworkImageView);
        }
    }

    private void syncTrackUiWithNotification(final int action){
        if(getActivity() == null){
            return;
        }

        View rootView;

        // Determine if the layout is two pane layout or not.
        boolean isLargeLayout = getResources().getBoolean(R.bool.large_layout);

        if(isLargeLayout){
            try {
                rootView = getActivity().getSupportFragmentManager().findFragmentByTag(getString(R.string.TAG_PLAYER)).getView().findViewById(R.id.track_player_linearlayout);
            }catch(NullPointerException e){
                rootView = null;
                e.printStackTrace();
            }
        }else {
            rootView = getActivity().findViewById(R.id.track_player_linearlayout);
        }

        switch(action){
            case MusicStatus.UPDATE_TRACK_UI_SKIP_TO_PREV:
                mCurrentTrackId = (mCurrentTrackId > 0 ? --mCurrentTrackId : 0);
                mCurrentTrack = mTracksInDB.get(mCurrentTrackId);

                if(rootView != null){
                    updateTrackUI(rootView, mCurrentTrack);
                }

                break;
            case MusicStatus.UPDATE_TRACK_UI_SKIP_TO_NEXT:
                int lastTrackId = mTracksInDB.size() - 1;
                // Determine the appropriate track id.
                mCurrentTrackId = (mCurrentTrackId < lastTrackId ? ++mCurrentTrackId : lastTrackId);
                mCurrentTrack = mTracksInDB.get(mCurrentTrackId);

                if(rootView != null){
                    updateTrackUI(rootView, mCurrentTrack);
                }

                break;
            default:
                break;
        }
    }

    /**
     * Using the LocalBraodcastManager to broadcast actions to the registered receiver within this app.
     * @param action
     */
    private void sendBroadcast(String action) {

        Intent intent = new Intent();

        switch (action) {
            case MusicPlayer.PLAY:
                // Tell the user that the track is buffering instead of showing nothing.
                showBufferingProgressDialog();
                break;

            case MusicPlayer.PAUSE:
                // No extra data is required for this action.
                break;

            case MusicPlayer.PREV:
                // Tell the user that the track is buffering instead of showing nothing.
                showBufferingProgressDialog();
                break;

            case MusicPlayer.NEXT:
                // Tell the user that the track is buffering instead of showing nothing.
                showBufferingProgressDialog();
                break;

            case MusicPlayer.SEEK_TO:
                intent.putExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_SEEK_TO), mSeekBar.getProgress());
                break;

            default:
                // Don't response to the undefined action.
                return;
        }

        intent.setAction(action);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    /**
     * Change the image button to display 'play' or 'pause'.
     * @param isPlaying
     */
    private void updatePlayPauseImageButton(boolean isPlaying) {
        if (isPlaying) {
            // The song is playing now, set the image src to "pause".
            mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_pause);
            mIsPlaying = true;
        } else {
            // The song is not playing now, set the image to "play".
            mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
            mIsPlaying = false;
        }
    }

    /**
     * Update the status of seek bar.
     * @param seekBar
     * @param currentPos
     * @param duration
     * @param startTextView
     * @param endTextView
     */
    private void updateTrackProgress(SeekBar seekBar, int currentPos, int duration,
                                     TextView startTextView, TextView endTextView) {
        seekBar.setProgress(currentPos);
        seekBar.setMax(duration); // Set the song duration.

        // Set the elapsed time.
        int minutes = currentPos / MILLISECONDS_PER_MINUTE;
        int seconds = (currentPos % MILLISECONDS_PER_MINUTE) / MILLISECONDS_PER_SECOND;
        String strElapsed = String.format("%d:%02d", minutes, seconds);
        startTextView.setText(strElapsed);

        // Set the end of the song.
        minutes = duration / MILLISECONDS_PER_MINUTE;
        seconds = (duration % MILLISECONDS_PER_MINUTE) / MILLISECONDS_PER_SECOND;
        String strDuration = String.format("%d:%02d", minutes, seconds);
        endTextView.setText(strDuration);
    }

    private void showBufferingProgressDialog(){
        mBufferingProgressDialog = ProgressDialog
                .show(getActivity(), mCurrentTrack.getArtist_name(), "Track buffering...");
    }

    private void dismissBufferingProgressDialog(){
        if(mBufferingProgressDialog != null && mBufferingProgressDialog.isShowing()){
            mBufferingProgressDialog.dismiss();
            mBufferingProgressDialog = null;
        }
    }
}
