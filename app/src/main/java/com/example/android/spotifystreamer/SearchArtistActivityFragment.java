package com.example.android.spotifystreamer;

import android.content.Context;
import android.content.Intent;
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
import android.view.inputmethod.InputMethodManager;
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


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchArtistActivityFragment extends Fragment {

    //public final String LOG_TAG = getClass().getSimpleName();
    private ArtistAdapter mArtistAdapter;
    private InputMethodManager imm;

    public SearchArtistActivityFragment() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the existing artists info for restoring later.
        outState.putParcelableArrayList(
                getString(R.string.parcel_artists_result),
                (ArrayList<SpotifyArtist>) mArtistAdapter.getArtists());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        // Restore artists info if available.
        if (savedInstanceState != null) {
            List<SpotifyArtist> list = savedInstanceState.getParcelableArrayList(
                    getString(R.string.parcel_artists_result));

            if (list != null) {
                mArtistAdapter.setArtists(list);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_artist, container, false);

        imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);

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
                SpotifyArtist SpotifyArtist = (SpotifyArtist) parent.getItemAtPosition(position);

                // Pass artist id and name to TopTracksActivity via Intent.
                if (SpotifyArtist != null) {
                    Intent topTrackIntent = new Intent(getActivity(), TopTracksActivity.class);
                    topTrackIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            new String[]{SpotifyArtist.getId(), SpotifyArtist.getName()}
                    );
                    startActivity(topTrackIntent);
                }
            }
        });

        final EditText editText = (EditText) rootView.findViewById(R.id.search_artist_edittext);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Hide the soft input window.
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

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

        @Override
        protected List<SpotifyArtist> doInBackground(String... params) {
            List<SpotifyArtist> SpotifyArtists = null;
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
                    SpotifyArtists = new ArrayList<>();
                    for (Artist artist : artistsPager.artists.items) {
                        String name = artist.name;
                        String id = artist.id;
                        String image_url = "";

                        int numImages = artist.images.size();
                        if (numImages > 0) {
                            // looking for an appropriate thumbnail image for artist list item.
                            image_url = Utils.findImageForListItem(artist.images);
                        }

                        SpotifyArtists.add(new SpotifyArtist(id, name, image_url));
                    }
                }
            }

            return SpotifyArtists;
        }

        @Override
        protected void onPostExecute(List<SpotifyArtist> SpotifyArtists) {

            if(getActivity() != null) {

                mArtistAdapter.clear();

                // Update artists.
                if (SpotifyArtists != null) {
                    mArtistAdapter.setArtists(SpotifyArtists);
                }
                // Error happened during searching artists.
                else if (failedToFetchData) {
                    Utils.displayToast(getActivity(),R.string.error_failed_to_fetch_data,
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
