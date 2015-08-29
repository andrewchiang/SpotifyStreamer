package com.example.android.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class SearchArtistActivity extends ActionBarActivity
        implements SearchArtistActivityFragment.OnArtistSelectedListener{

    private static final String LOG_TAG = SearchArtistActivity.class.getSimpleName();
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_artist);

        // Checks if the layout is single pane or two pane.
        if(findViewById(R.id.tracks_container) != null){
            mTwoPane = true;
        }else{
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_artist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This activity implements the interface declaring in the SearchArtistActivityFragment.
     * @param args: This bundle contains an artist id and artist name.
     */
    @Override
    public void onArtistSelected(Bundle args) {

        if(mTwoPane){
            // Adds the top tracks fragment dynamically if this layout is two pane.
            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tracks_container, fragment, getString(R.string.TAG_TOP_TRACKS_FRAGMENT))
                    .commit();
        }else{
            // Creates a TopTracksActivity and passes the data to it.
            Intent intent = new Intent(this, TopTracksActivity.class);
            intent.putExtra(getString(R.string.INTENT_KEY_ARTIST_ID_AND_NAME), args);
            startActivity(intent);
        }
    }
}
