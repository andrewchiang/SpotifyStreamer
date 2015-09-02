package com.example.android.spotifystreamer;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.spotifystreamer.data.TopTracksContract;
import com.example.android.spotifystreamer.player.PlayerActivity;
import com.example.android.spotifystreamer.player.PlayerDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksActivityFragment extends Fragment {

    private static final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();
    private boolean mIsLargeLayout;
    private String mArtistName;
    private String mArtistId;

    private TrackAdapter mTrackAdapter;

    public TopTracksActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the existing top tracks info for restoring later.
        outState.putParcelableArrayList(getString(R.string.PARCEL_KEY_TOP_TRACKS_RESULT),
                (ArrayList<SpotifyTrack>) mTrackAdapter.getTracks());

        // Save the subtitle (artist name) of ActionBar.
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            outState.putString(getString(R.string.PARCEL_KEY_ACTION_BAR_SUBTITLE),
                    actionBar.getSubtitle().toString());
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore top tracks info if available.
            List<SpotifyTrack> tracks = savedInstanceState.
                    getParcelableArrayList(getString(R.string.PARCEL_KEY_TOP_TRACKS_RESULT));

            if (tracks != null) {
                mTrackAdapter.setTracks(tracks);
            }

            // Restore the subtitle of ActionBar.
            String subtitle = savedInstanceState.getString(getString(R.string.PARCEL_KEY_ACTION_BAR_SUBTITLE));
            ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        // Initialize the TrackAdapter.
        mTrackAdapter = new TrackAdapter(getActivity(), new ArrayList<SpotifyTrack>());
        // Get the list view.
        ListView listView = (ListView) rootView.findViewById(R.id.top_10_tracks_listview);
        // Bind list view with TrackAdapter.
        listView.setAdapter(mTrackAdapter);

        // The music player will be launched when the user choose a track.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long trackPosInDb = id;
                Log.d(LOG_TAG, "trackPosInDb: " + trackPosInDb);
                showPlayerDialog(trackPosInDb);
            }
        });

        // Search top 10 tracks if restore is not available.
        if (savedInstanceState == null) {

            Bundle args = getArguments();
            if (args != null) {
                try {
                    mArtistId = args.getStringArray(getString(R.string.BUNDLE_KEY_ARTIST_ID_AND_NAME))[0];
                    mArtistName = args.getStringArray(getString(R.string.BUNDLE_KEY_ARTIST_ID_AND_NAME))[1];

                    // Call getSupportActionBar() instead of getActionBar
                    // when using AppCompat support library
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setSubtitle(mArtistName);
                        }
                    }

                    // Search the top 10 tracks based on the artist id.
                    SearchTopTracksTask searchTopTracksTask = new SearchTopTracksTask();
                    searchTopTracksTask.execute(mArtistId);
                } catch (NullPointerException e) {

                    // Error caused by trying to get artist id and name.
                    Log.d(LOG_TAG, "onCreateView: " + e.getMessage());

                    Utils.displayToast(getActivity(),
                            R.string.error_invalid_artist_id_or_name,
                            Toast.LENGTH_SHORT,
                            Gravity.CENTER
                    );
                }
            }
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /**
         * Set this variable to true or false based on the screen size.
         * large_layout is defined in two places:
         *     - res/values/bools.xml
         *     - res/values-large/bools.xml
         * This variable will be used in showPlayerDialog().
         */
        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);
    }

    public class SearchTopTracksTask extends AsyncTask<String, Void, List<SpotifyTrack>> {
        //private final String LOG_TAG = SearchTopTracksTask.class.getSimpleName();
        private boolean failedToFetchData = false;

        @Override
        protected List<SpotifyTrack> doInBackground(String... params) {
            List<SpotifyTrack> spotifyTracks = null;
            Tracks tracks = null;

            SpotifyService spotifyService = new SpotifyApi().getService();

            // Country code is required for retrieving top tracks.
            Map<String, Object> options = new HashMap<>();
            options.put(SpotifyService.COUNTRY, Locale.getDefault().getCountry());

            // Searching top tracks.
            try {
                tracks = spotifyService.getArtistTopTrack(params[0], options);
            } catch (RetrofitError re) {
                failedToFetchData = true;
            }

            // Get top tracks from spotify.
            if (tracks != null) {
                if (getActivity() != null && !tracks.tracks.isEmpty()) {
                    spotifyTracks = new ArrayList<>();

                    for (Track track : tracks.tracks) {
                        String small_image_url = ""; // image for track list item.
                        String large_image_url = ""; // image for streaming audio in stage 2.
                        int numImages = track.album.images.size();

                        if (numImages > 0) {

                            // looking for an appropriate thumbnail image for list item.
                            small_image_url = Utils.findImageForListItem(track.album.images);

                            // set image to the largest one, which is the first image.
                            large_image_url = track.album.images.get(0).url;
                        }

                        spotifyTracks.add(
                                new SpotifyTrack(
                                        mArtistName,
                                        track.name,
                                        track.album.name,
                                        small_image_url,
                                        large_image_url,
                                        track.preview_url
                                )
                        );
                    }

                    // Inserts all tracks getting from Spotify endpoint into the database.
                    insertTracksToDb(spotifyTracks);
                }
            }

            return spotifyTracks;
        }

        @Override
        protected void onPostExecute(List<SpotifyTrack> spotifyTracks) {

            if (getActivity() != null) {

                mTrackAdapter.clear();

                // Update top tracks.
                if (getActivity() != null && spotifyTracks != null) {
                    mTrackAdapter.setTracks(spotifyTracks);
                }
                // Error happened during searching top tracks.
                else if (failedToFetchData) {
                    Utils.displayToast(getActivity(), R.string.error_failed_to_fetch_data,
                            Toast.LENGTH_SHORT, Gravity.CENTER);
                }
                // Top tracks are not available.
                else {
                    Utils.displayToast(getActivity(), R.string.error_no_top_tracks_not_found,
                            Toast.LENGTH_SHORT, Gravity.CENTER);
                }
            }
        }

        private void insertTracksToDb(List<SpotifyTrack> tracks) {

            int rowsDeleted = Utils.deleteTracksFromDb(
                    getActivity(),
                    TopTracksContract.TopTracksEntry.CONTENT_URI,
                    TopTracksContract.TopTracksEntry.TABLE_NAME
            );

            Log.d(LOG_TAG, "Total has " + rowsDeleted + " records deleted from table.");

            int count = tracks.size();

            if (count > 0) {

                ContentValues[] values = new ContentValues[count];

                for (int i = 0; i < count; ++i) {
                    ContentValues value = new ContentValues();
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_ARTIST, tracks.get(i).getArtist_name());
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_ALBUM, tracks.get(i).getAlbum_name());
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_TRACK, tracks.get(i).getTrack_name());
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE, tracks.get(i).getSmall_image_url());
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE, tracks.get(i).getLarge_image_url());
                    value.put(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL, tracks.get(i).getPreview_url());
                    values[i] = value;
                }

                int numInserted = getActivity().getContentResolver().bulkInsert(
                        TopTracksContract.TopTracksEntry.CONTENT_URI,
                        values
                );

                Log.d(LOG_TAG, "Total has " + numInserted + " records inserted into table");
            }
        }
    }

    /**
     * This function is used for display the music player.
     * Player will be displayed as full screen or embedded fragment based on the screen size.
     */
    private void showPlayerDialog(long trackId) {

        Bundle args = new Bundle();
        args.putLong(getString(R.string.BUNDLE_KEY_PLAYER_TRACK_POS), trackId);

        if (mIsLargeLayout) {
            // The device is using a large layout, so show the fragment as a dialog

            FragmentManager fm = getActivity().getSupportFragmentManager();
            PlayerDialogFragment dialogFragment = new PlayerDialogFragment();
            dialogFragment.setArguments(args);
            dialogFragment.show(fm, getString(R.string.TAG_PLAYER));
        } else {
            // The device is smaller, so show the fragment fullscreen

            Intent playerIntent = new Intent(getActivity(), PlayerActivity.class);
            playerIntent.putExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS), args);
            startActivity(playerIntent);
        }
    }


}
