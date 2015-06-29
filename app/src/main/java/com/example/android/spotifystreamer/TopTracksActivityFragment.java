package com.example.android.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

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

    //private final String LOG_TAG = getClass().getSimpleName();
    private TrackAdapter mTrackAdapter;

    public TopTracksActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the existing top tracks info for restoring later.
        outState.putParcelableArrayList(getString(R.string.parcel_top_tracks_result),
                (ArrayList<SpotifyTrack>) mTrackAdapter.getTracks());

        // Save the subtitle (artist name) of ActionBar.
        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if(actionBar != null){
            outState.putString(getString(R.string.parcel_actionbar_subtitle),
                    actionBar.getSubtitle().toString());
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            // Restore top tracks info if available.
            List<SpotifyTrack> tracks = savedInstanceState.
                    getParcelableArrayList(getString(R.string.parcel_top_tracks_result));

            if (tracks != null) {
                mTrackAdapter.setTracks(tracks);
            }

            // Restore the subtitle of ActionBar.
            String subtitle = savedInstanceState.getString(getString(R.string.parcel_actionbar_subtitle));
            ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
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

        // Search top 10 tracks if restore is not available.
        if(savedInstanceState == null) {
            // Retrieve info from intent passed by ArtistSearchActivityFragment.
            Intent topTrackIntent = getActivity().getIntent();
            // Make sure intent is available and has data (a String array) in it.
            if (topTrackIntent != null && topTrackIntent.hasExtra(Intent.EXTRA_TEXT)) {

                String artistId = topTrackIntent.getStringArrayExtra(Intent.EXTRA_TEXT)[0];
                String artistName = topTrackIntent.getStringArrayExtra(Intent.EXTRA_TEXT)[1];

                // Call getSupportActionBar() instead of getActionBar
                // when using AppCompat support library
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setSubtitle(artistName);
                    }
                }

                // Search the top 10 tracks based on the artist id.
                SearchTopTracksTask searchTopTracksTask = new SearchTopTracksTask();
                searchTopTracksTask.execute(artistId);
            }
        }

        return rootView;
    }

    public class SearchTopTracksTask extends AsyncTask<String, Void, List<SpotifyTrack>> {
        //private final String LOG_TAG = SearchTopTracksTask.class.getSimpleName();
        private boolean failedToFetchData = false;

        @Override
        protected List<SpotifyTrack> doInBackground(String... params) {
            List<SpotifyTrack> SpotifyTracks = null;
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
                    SpotifyTracks = new ArrayList<>();

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

                        SpotifyTracks.add(new SpotifyTrack(track.name, track.album.name, small_image_url, large_image_url, track.preview_url));
                    }
                }
            }

            return SpotifyTracks;
        }

        @Override
        protected void onPostExecute(List<SpotifyTrack> SpotifyTracks) {

            if(getActivity() != null) {

                mTrackAdapter.clear();

                // Update top tracks.
                if (getActivity() != null && SpotifyTracks != null) {
                    mTrackAdapter.setTracks(SpotifyTracks);
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
    }
}
