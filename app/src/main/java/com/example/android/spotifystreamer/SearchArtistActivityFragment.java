package com.example.android.spotifystreamer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;

public class SearchArtistActivityFragment extends Fragment {

    public static final String LOG_TAG = SearchArtistActivityFragment.class.getSimpleName();
    private ArtistAdapter mArtistAdapter;

    /**
     * SearchArtistActivity must implement this interface in order to handle the event.
     */
    public interface OnArtistSelectedListener {
        void onArtistSelected(Bundle args);
    }

    public SearchArtistActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the existing artists info for restoring later.
        outState.putParcelableArrayList(
                getString(R.string.PARCEL_KEY_ARTISTS_RESULT),
                (ArrayList<SpotifyArtist>) mArtistAdapter.getArtists());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        // Restore artists info if available.
        if (savedInstanceState != null) {
            List<SpotifyArtist> list = savedInstanceState.getParcelableArrayList(
                    getString(R.string.PARCEL_KEY_ARTISTS_RESULT));

            if (list != null) {
                mArtistAdapter.setArtists(list);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_artist, container, false);

        // Initialize ArtistAdapter.
        mArtistAdapter = new ArtistAdapter(getActivity(), new ArrayList<SpotifyArtist>());

        // Find the list view.
        ListView listView = (ListView) rootView.findViewById(R.id.artists_listview);

        // Bind list view with ArtistAdapter.
        listView.setAdapter(mArtistAdapter);

        // Register callback function.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Retrieve the selected artist.
                SpotifyArtist spotifyArtist = (SpotifyArtist) parent.getItemAtPosition(position);

                // Pass artist id and name to TopTracksActivity via Intent.
                if (spotifyArtist != null) {

                    Activity activity = getActivity();

                    if (activity instanceof OnArtistSelectedListener) {
                        Bundle args = new Bundle();
                        args.putStringArray(getString(R.string.BUNDLE_KEY_ARTIST_ID_AND_NAME),
                                new String[]{spotifyArtist.getId(), spotifyArtist.getName()}
                        );

                        ((OnArtistSelectedListener) activity).onArtistSelected(args);
                    }
                }
            }
        });

        final EditText editText = (EditText) rootView.findViewById(R.id.search_artist_edittext);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String artist_name = v.getText().toString().trim();

                    // Search artists when the artist name is specified.
                    if (!artist_name.isEmpty()) {
                        SearchArtistTask searchArtistTask = new SearchArtistTask();
                        searchArtistTask.execute(artist_name);
                    }
                }
                return false;
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                editText.setText("");

                return false;
            }
        });

        return rootView;
    }

    public class SearchArtistTask extends AsyncTask<String, Void, List<SpotifyArtist>> {

        //private final String LOG_TAG = SearchArtistTask.class.getSimpleName();
        private boolean failedToFetchData = false;

        // Display the progress dialog to inform the user that the search is running.
        private ProgressDialog mProgressDialog =
                ProgressDialog.show(getActivity(), "", "Searching artist...");

        @Override
        protected List<SpotifyArtist> doInBackground(String... params) {
            List<SpotifyArtist> spotifyArtists = null;
            ArtistsPager artistsPager = null;

            SpotifyService spotifyService = new SpotifyApi().getService();

            // Searching artists.
            try {
                artistsPager = spotifyService.searchArtists(params[0]);
            } catch (RetrofitError re) {
                failedToFetchData = true;
            }

            // Get artists from spotify.
            if (artistsPager != null) {
                if (getActivity() != null && artistsPager.artists.total != 0) {
                    spotifyArtists = new ArrayList<>();
                    for (Artist artist : artistsPager.artists.items) {
                        String name = artist.name;
                        String id = artist.id;
                        String image_url = "";

                        int numImages = artist.images.size();
                        if (numImages > 0) {
                            // looking for an appropriate thumbnail image for artist list item.
                            image_url = Utils.findImageForListItem(artist.images);
                        }

                        spotifyArtists.add(new SpotifyArtist(id, name, image_url));
                    }
                }
            }

            return spotifyArtists;
        }

        @Override
        protected void onPostExecute(List<SpotifyArtist> spotifyArtists) {

            if (getActivity() != null) {

                // Dismiss the progress dialog after the searching task is done.
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                mArtistAdapter.clear();

                // Update artists.
                if (spotifyArtists != null) {
                    mArtistAdapter.setArtists(spotifyArtists);
                }
                // Error happened during searching artists.
                else if (failedToFetchData) {
                    Utils.displayToast(getActivity(), R.string.error_failed_to_fetch_data,
                            Toast.LENGTH_SHORT, Gravity.CENTER);
                }
                // Artists are not available.
                else {
                    Utils.displayToast(getActivity(), R.string.error_no_artists_are_found,
                            Toast.LENGTH_SHORT, Gravity.CENTER);
                }
            }
        }
    }
}
