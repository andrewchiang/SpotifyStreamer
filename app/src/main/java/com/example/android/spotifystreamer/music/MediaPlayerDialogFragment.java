package com.example.android.spotifystreamer.music;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.spotifystreamer.R;
import com.example.android.spotifystreamer.Utils;
import com.example.android.spotifystreamer.data.TopTracksContract;
import com.squareup.picasso.Picasso;

/**
 * Created by achiang on 8/24/15.
 */
public class MediaPlayerDialogFragment extends DialogFragment {

    private static final String LOG_TAG = MediaPlayerDialogFragment.class.getSimpleName();
    private static final String PLAYER_TAG = "PLAYER_TAG";

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

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        PlayerInfo playerInfo = new PlayerInfo(
//                mArtistTextView.getText(),
//                mAlbumTextView.getText(),
//                mTrackTextView.getText(),
//                mArtworkImageView.
//
//        );
//        outState.putParcelable(PLAYER_TAG, playerInfo);
//    }
//
//    @Override
//    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
//        super.onViewStateRestored(savedInstanceState);
//
//        if(savedInstanceState != null){
//            PlayerInfo playerInfo = savedInstanceState.getParcelable(PLAYER_TAG);
//            if(playerInfo != null) {
//                mArtistTextView.setText(playerInfo.getArtist());
//                mAlbumTextView.setText(playerInfo.getAlbum());
//                mTrackTextView.setText(playerInfo.getTrack());
//
//                String imageUrl = playerInfo.getImage();
//                if (!imageUrl.isEmpty()) {
//                    Picasso.with(getActivity())
//                            .load(imageUrl)
//                            .resizeDimen(R.dimen.player_artwork, R.dimen.player_artwork)
//                            .centerCrop()
//                            .into(mArtworkImageView);
//                }
//
//                mEndTextView.setText(playerInfo.getDuration());
//                // SeekBar
//                mPreviousImageButton.setEnabled(playerInfo.getToPrev());
//
//                if (playerInfo.getPlaying()) {
//                    mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_pause);
//                } else {
//                    mPlayPauseIamgeButton.setImageResource(android.R.drawable.ic_media_play);
//                }
//
//                mNextImageButton.setEnabled(playerInfo.getToNext());
//            }
//        }
//    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.track_player, container, false);

        if(savedInstanceState == null) {
            Bundle args = getArguments();
            long trackId = args.getLong("trackId");

            Cursor cursor = getActivity().getContentResolver().query(
                    TopTracksContract.TopTracksEntry.buildTrackUri(trackId),
                    null,
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                Log.d(LOG_TAG, "cursor is not null");
                if (cursor.moveToFirst()) {
                    Log.d(LOG_TAG, "cursor is moving the first record");

                    String artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
                    String album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
                    String track = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
                    String small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
                    String large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
                    String preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));

                    mArtistTextView = (TextView) rootView.findViewById(R.id.artist_name_textview);
                    mArtistTextView.setText(artist);

                    mAlbumTextView = (TextView) rootView.findViewById(R.id.album_name_textview);
                    mAlbumTextView.setText(album);

                    mTrackTextView = (TextView) rootView.findViewById(R.id.track_name_textview);
                    mTrackTextView.setText(track);

                    mArtworkImageView = (ImageView) rootView.findViewById(R.id.album_artwork_imageview);
                    if (!large_artwork.isEmpty()) {
                        Picasso.with(getActivity())
                                .load(large_artwork)
                                .resizeDimen(R.dimen.player_artwork, R.dimen.player_artwork)
                                .centerCrop()
                                .into(mArtworkImageView);
                    }


                    mSeekBar = (SeekBar) rootView.findViewById(R.id.duration_seekbar);
                    mStartTextView = (TextView) rootView.findViewById(R.id.duration_start_textview);
                    mEndTextView = (TextView) rootView.findViewById(R.id.duration_end_textview);
                    mPreviousImageButton = (ImageButton) rootView.findViewById(R.id.previous_imagebutton);
                    mPlayPauseIamgeButton = (ImageButton) rootView.findViewById(R.id.play_pause_imagebutton);

//                mPlayPauseIamgeButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (mMusicService != null) {
//                            mMusicService.playTrack(previewUrl);
//                        }
//                    }
//                });

                    mNextImageButton = (ImageButton) rootView.findViewById(R.id.next_imagebutton);
                } else {
                    Log.d(LOG_TAG, "cursor is not moving to the first record.");
                }

                cursor.close();
            } else {
                Utils.displayToast(getActivity(),
                        R.string.error_track_is_not_available,
                        Toast.LENGTH_SHORT,
                        Gravity.CENTER);
            }

        }
        return rootView;
    }
}
