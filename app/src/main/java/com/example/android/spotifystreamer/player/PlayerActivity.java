package com.example.android.spotifystreamer.player;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.spotifystreamer.R;

/**
 * Created by achiang on 8/29/15.
 */
public class PlayerActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activiy_player);

        if(savedInstanceState == null){

            // Gets the bundle passing by TopTracksActivityFragment.
            Bundle args = getIntent().getBundleExtra(getString(R.string.INTENT_KEY_PLAYER_TRACK_POS));

            // Creates a player dialog fragment and passes the bundle to it.
            PlayerDialogFragment playerDialogFragment = new PlayerDialogFragment();
            playerDialogFragment.setArguments(args);

            // Adds the player dialog fragment dynamically.
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.player_container,
                            playerDialogFragment,
                            getString(R.string.TAG_PLAYER))
                    //.addToBackStack(null)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
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
}
