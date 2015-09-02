package com.example.android.spotifystreamer.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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
import com.example.android.spotifystreamer.data.TopTracksContract;
import com.example.android.spotifystreamer.service.MusicService;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by achiang on 8/24/15.
 */
public class PlayerDialogFragment extends DialogFragment
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener{

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

    /* variables about player status */
    private boolean mIsPlaying;
    private int mCurrentPos;
    private int mMaxDuration;

    /* variables about track */
    private int mCurrentTrackId;
    private SpotifyTrack mCurrentTrack;
    private List<SpotifyTrack> mTracksInDB = new ArrayList<>();

    /* variables about service */
    private MusicService mMusicService;
    private boolean mBound;


    /**
     * To solve "The Handler should be static or leaks might occur" problem.
     * The solution is shown bellow:
     * Romain Guy -> https://groups.google.com/forum/?fromgroups=#%21msg/android-developers/1aPZXZG6kWk/lIYDavGYn5UJ
     * Alex Lockwood -> http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
     */
    private UiHandler mUiHandler = new UiHandler(this);

    private static class UiHandler extends Handler {
        private final WeakReference<PlayerDialogFragment> playerDialogFragmentWeakReference;

        public UiHandler(PlayerDialogFragment playerDialogFragment) {
            playerDialogFragmentWeakReference = new WeakReference<PlayerDialogFragment>(playerDialogFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            PlayerDialogFragment dialogFragment = playerDialogFragmentWeakReference.get();

            if (dialogFragment != null) {
                switch(msg.what){
                    case PlayerAction.PLAY:
                        Log.d(LOG_TAG, "msg.what = " + msg.what);
                        dialogFragment.mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_pause);
                        dialogFragment.mIsPlaying = true;
                        break;

                    case PlayerAction.PAUSE:
                        Log.d(LOG_TAG, "msg.what = " + msg.what);
                        dialogFragment.mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
                        dialogFragment.mIsPlaying = false;
                        break;

                    case PlayerAction.STOP:
                        Log.d(LOG_TAG, "msg.what = " + msg.what);
                        dialogFragment.mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
                        dialogFragment.mIsPlaying = false;
                        break;

                    case PlayerAction.DONE:
                        Log.d(LOG_TAG, "msg.what = " + msg.what);
                        dialogFragment.mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
                        dialogFragment.mIsPlaying = false;
                        break;

                    case PlayerAction.PROGRESS:
                        //Log.d(LOG_TAG, "msg.what = " + msg.what + "msg.arg1 = " + msg.arg1 + "msg.arg2 = " + msg.arg2);

                        int currentPos = msg.arg1;
                        int duration = msg.arg2;

                        dialogFragment.updateTrackProgress(dialogFragment.mSeekBar, currentPos, duration,
                                dialogFragment.mStartTextView, dialogFragment.mEndTextView);
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
            Log.d(LOG_TAG, "onServiceConnected");

            mBound = true;
            mMusicService = ((MusicService.MusicBinder) service).getService();
            mMusicService.startPlayback(mCurrentTrack.getPreview_url());
            mMusicService.registerHandler(mUiHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(LOG_TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(LOG_TAG, "onSaveInstanceState");

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

        Log.d(LOG_TAG, "onStart");

        Intent serviceIntent = new Intent(getActivity(), MusicService.class);

        //serviceIntent.putExtra(getString(R.string.INTENT_KEY_SERVICE_PREVIEW_URL), mCurrentTrack.getPreview_url());

        getActivity().startService(serviceIntent);
        getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(LOG_TAG, "onStop");

        if (mBound) {
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");

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
            getTracksFromDb();

            // Get the selected track id.
            mCurrentTrackId = (int) getArguments().getLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS));

            // Set the current position and max duration to default.
            mCurrentPos = 0;
            mMaxDuration = TEMP_MAX_DURATION;
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
            mCurrentPos = savedInstanceState
                    .getInt(getString(R.string.PARCEL_KEY_SEEKBAR_CURRENT_POS));
            // Restore the duration of the track.
            mMaxDuration = savedInstanceState
                    .getInt(getString(R.string.PARCEL_KEY_SEEKBAR_MAX));
        }

        if(mIsPlaying){
            mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_pause);
        }else{
            mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
        }

        // Set the current track based on the current track id.
        mCurrentTrack = mTracksInDB.get(mCurrentTrackId);

        // Update the track info based on the current track.
        updateTrackUI(rootView, mCurrentTrack);

        // Update the progress of the SeekBar.
        updateTrackProgress(mSeekBar, mCurrentPos, mMaxDuration, mStartTextView, mEndTextView);

//        if(savedInstanceState == null) {
//            mCurrentTrackId = (int) getArguments().getLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS));
//            Log.d(LOG_TAG, "savedInstanceState is null, mCurrentTrackId is " + mCurrentTrackId);
//        }
//
//        Log.d(LOG_TAG, "mCurrentTrackId is " + mCurrentTrackId);
//        mCurrentTrack = mTracksInDB.get(mCurrentTrackId);
//        updateTrackUI(rootView, mCurrentTrack);

        //ToDo: bypass the restoration for now.
//        //if (savedInstanceState == null) {
//
//            Bundle args = getArguments();
//
//            // Get the selected track.
//            long trackPos = args.getLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS));
//            Log.d(LOG_TAG, "onCreateView: track position = " + trackPos);
//
//            // Query database based on the selected track.
//            Cursor cursor = getActivity().getContentResolver().query(
//                    TopTracksContract.TopTracksEntry.buildTrackUri(trackPos),
//                    null,
//                    null,
//                    null,
//                    null
//            );
//
//            // Cursor is positioning before the first entry.
//            if (cursor != null && cursor.moveToFirst()) {
//                Log.d(LOG_TAG, "cursor is not null and moved to the first entry.");
//
//                updateTrackUI(cursor);
//                updateTrackUI(rootView, mCurrentTrack);
//
//                cursor.close();
//            } else {
//                // No record.
//                Utils.displayToast(getActivity(),
//                        R.string.error_track_is_not_available,
//                        Toast.LENGTH_SHORT,
//                        Gravity.CENTER);
//            }
//        //}

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.previous_imagebutton:
                if (mCurrentTrackId > 0) {
                    mCurrentTrackId--;
                    mCurrentTrack = mTracksInDB.get(mCurrentTrackId);
                    mMusicService.prevPlayback(mCurrentTrack.getPreview_url());
                } else {
                    Log.d(LOG_TAG, "Reached the beginning of playlist");
                }

                updateTrackUI(v.getRootView(), mCurrentTrack);
                updateTrackProgress(mSeekBar, 0, TEMP_MAX_DURATION, mStartTextView, mEndTextView);
                break;

            case R.id.play_pause_imagebutton:

                if(mMusicService.musicIsPlaying()){
                    mMusicService.pausePlayback();
                }else{
                    mMusicService.resumePlayback();
                }

                break;

            case R.id.next_imagebutton:
                if (mCurrentTrackId < mTracksInDB.size() - 1) {
                    mCurrentTrackId++;
                    mCurrentTrack = mTracksInDB.get(mCurrentTrackId);
                    mMusicService.nextPlayback(mCurrentTrack.getPreview_url());
                } else {
                    Log.d(LOG_TAG, "Reached the end of playlist");
                }

                updateTrackUI(v.getRootView(), mCurrentTrack);
                updateTrackProgress(mSeekBar, 0, TEMP_MAX_DURATION, mStartTextView, mEndTextView);
                break;

            default:
                Log.d(LOG_TAG, "Undefined view id");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(LOG_TAG, "seekTo : " + seekBar.getProgress());
        mMusicService.seekTo(seekBar.getProgress());
    }

    /**
     * Retrieves all tracks from 'tracks' table in database
     * , and stores those tracks into mTracksInDB.
     */
    private void getTracksFromDb() {

        Log.d(LOG_TAG, "getTracksFromDb");

        Cursor cursor = getActivity().getContentResolver().query(
                TopTracksContract.TopTracksEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
                String track_name = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
                String small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
                String large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
                String preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));

                SpotifyTrack track = new SpotifyTrack(
                        artist,
                        track_name,
                        album,
                        small_artwork,
                        large_artwork,
                        preview_url
                );
                mTracksInDB.add(track);
            } while (cursor.moveToNext());

            cursor.close();

            Log.d(LOG_TAG, "Total has " + mTracksInDB.size() + " songs added");

        } else {
            Log.d(LOG_TAG, "No tracks available.");
            //ToDo: Notify user tracks are not available.
        }
    }

    private void updateTrackUI(View rootView, SpotifyTrack track) {

        // Keep referencing to the current track.
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

    private void updateTrackProgress(SeekBar seekBar, int currentPos, int duration,
                                     TextView startTextView, TextView endTextView){
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


//    private void updateTrackUI(Cursor cursor) {
//        String artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
//        String album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
//        String track_name = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
//        String small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
//        String large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
//        String preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));
//
//        if(mCurrentTrack != null){
//            mCurrentTrack = null;
//        }
//        mCurrentTrack = new SpotifyTrack(
//                artist,
//                album,
//                track_name,
//                small_artwork,
//                large_artwork,
//                preview_url
//        );
//    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        Log.d(LOG_TAG, "in onSaveInstanceState");
//        outState.putParcelable(getString(R.string.PARCEL_KEY_PLAYER_INFO), mPlayerInfoForRestore);
//    }
//
//    @Override
//    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
//        super.onViewStateRestored(savedInstanceState);
//
//        Log.d(LOG_TAG, "in onViewStateRestored");
//
//        if(savedInstanceState != null){
//            PlayerInfo playerInfo = savedInstanceState.getParcelable(getString(R.string.PARCEL_KEY_PLAYER_INFO));
//            updateTrackUI(playerInfo);
//        }
//    }
//
//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//
//        Log.d(LOG_TAG, "onActivityCreated");
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.track_player, container, false);
//        Log.d(LOG_TAG, "in onCreateView");
//
//        mArtistTextView = (TextView) rootView.findViewById(R.id.artist_name_textview);
//        mAlbumTextView = (TextView) rootView.findViewById(R.id.album_name_textview);
//        mTrackTextView = (TextView) rootView.findViewById(R.id.track_name_textview);
//        mArtworkImageView = (ImageView) rootView.findViewById(R.id.album_artwork_imageview);
//
//        if(savedInstanceState == null) {
//
//            Bundle args = getArguments();
//            long trackPos = args.getLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS));
//            Log.d(LOG_TAG, "onCreateView: track position = " + trackPos);
//
//            QueryTrackFromDbTask queryTrackFromDbTask = new QueryTrackFromDbTask();
//            queryTrackFromDbTask.execute(trackPos);
//
////            Cursor cursor = getActivity().getContentResolver().query(
////                    TopTracksContract.TopTracksEntry.buildTrackUri(trackId),
////                    null,
////                    null,
////                    null,
////                    null
////            );
////
////            if (cursor != null) {
////                Log.d(LOG_TAG, "cursor is not null");
////                if (cursor.moveToFirst()) {
////                    Log.d(LOG_TAG, "cursor is moving the first record");
////
////                    String artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
////                    String album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
////                    String track = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
////                    String small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
////                    String large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
////                    String preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));
////
////                    mArtistTextView = (TextView) rootView.findViewById(R.id.artist_name_textview);
////                    mArtistTextView.setText(artist);
////
////                    mAlbumTextView = (TextView) rootView.findViewById(R.id.album_name_textview);
////                    mAlbumTextView.setText(album);
////
////                    mTrackTextView = (TextView) rootView.findViewById(R.id.track_name_textview);
////                    mTrackTextView.setText(track);
////
////                    mArtworkImageView = (ImageView) rootView.findViewById(R.id.album_artwork_imageview);
////                    if (!large_artwork.isEmpty()) {
////                        Picasso.with(getActivity())
////                                .load(large_artwork)
////                                .resizeDimen(R.dimen.player_artwork, R.dimen.player_artwork)
////                                .centerCrop()
////                                .into(mArtworkImageView);
////                    }
////
////
////                    mSeekBar = (SeekBar) rootView.findViewById(R.id.duration_seekbar);
////                    mStartTextView = (TextView) rootView.findViewById(R.id.duration_start_textview);
////                    mEndTextView = (TextView) rootView.findViewById(R.id.duration_end_textview);
////                    mPreviousImageButton = (ImageButton) rootView.findViewById(R.id.previous_imagebutton);
////                    mPlayPauseIamgeButton = (ImageButton) rootView.findViewById(R.id.play_pause_imagebutton);
////
//////                mPlayPauseIamgeButton.setOnClickListener(new View.OnClickListener() {
//////                    @Override
//////                    public void onClick(View v) {
//////                        if (mMusicService != null) {
//////                            mMusicService.playTrack(previewUrl);
//////                        }
//////                    }
//////                });
////
////                    mNextImageButton = (ImageButton) rootView.findViewById(R.id.next_imagebutton);
////                } else {
////                    Log.d(LOG_TAG, "cursor is not moving to the first record.");
////                }
////
////                cursor.close();
////            } else {
////                Utils.displayToast(getActivity(),
////                        R.string.error_track_is_not_available,
////                        Toast.LENGTH_SHORT,
////                        Gravity.CENTER);
////            }
//
//        }
//        return rootView;
//    }
//
//    private void updateTrackUI(PlayerInfo playerInfo){
//        if(playerInfo != null){
//
//            // Refers to the instance of PlayerInfo.
//            mPlayerInfoForRestore = playerInfo;
//
//            mArtistTextView.setText(playerInfo.getArtist());
//            mAlbumTextView.setText(playerInfo.getAlbum());
//            mTrackTextView.setText(playerInfo.getTrack());
//            String artwork = playerInfo.getImage();
//            if (!artwork.isEmpty()) {
//                Picasso.with(getActivity())
//                        .load(artwork)
//                        .resizeDimen(R.dimen.player_artwork, R.dimen.player_artwork)
//                        .centerCrop()
//                        .into(mArtworkImageView);
//            }
//        }
//    }
//
//    public class QueryTrackFromDbTask extends AsyncTask<Long, Void, PlayerInfo>{
//        @Override
//        protected PlayerInfo doInBackground(Long... params) {
//            long trackId = params[0];
//            String artist;
//            String album;
//            String track;
//            String small_artwork;
//            String large_artwork;
//            String preview_url;
//
//            PlayerInfo playerInfo = null;
//
//            Cursor cursor = getActivity().getContentResolver().query(
//                    TopTracksContract.TopTracksEntry.buildTrackUri(trackId),
//                    null,
//                    null,
//                    null,
//                    null
//            );
//
//            if (cursor != null) {
//                Log.d(LOG_TAG, "cursor is not null");
//                if (cursor.moveToFirst()) {
//                    Log.d(LOG_TAG, "cursor is moving the first record");
//
//                    artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
//                    album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
//                    track = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
//                    small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
//                    large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
//                    preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));
//
//                    playerInfo = new PlayerInfo(
//                            artist,
//                            album,
//                            track,
//                            large_artwork,
//                            "",
//                            "",
//                            false,
//                            false,
//                            false
//                    );
//
//                } else {
//                    Log.d(LOG_TAG, "cursor is not moving to the first record.");
//                }
//
//                cursor.close();
//            } else {
//                Utils.displayToast(
//                        getActivity(),
//                        R.string.error_track_is_not_available,
//                        Toast.LENGTH_SHORT,
//                        Gravity.CENTER
//                );
//            }
//
//            return playerInfo;
//        }
//
//        @Override
//        protected void onPostExecute(PlayerInfo playerInfo) {
//            updateTrackUI(playerInfo);
//        }
//    }
}
