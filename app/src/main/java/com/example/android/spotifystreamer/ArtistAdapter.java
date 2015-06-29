package com.example.android.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by achiang on 6/29/15.
 */
public class ArtistAdapter extends ArrayAdapter<SpotifyArtist> {
    private List<SpotifyArtist> mArtists;

    public ArtistAdapter(Context context, ArrayList<SpotifyArtist> artists){
        super(context, 0, artists);
        mArtists = artists;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SpotifyArtist artist = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.artist_thumbnail_imageview);

        if(artist.getImage_url() != ""){
            Picasso.with(getContext())
                    .load(artist.getImage_url())
                    .resizeDimen(R.dimen.thumbnail,R.dimen.thumbnail)
                    .centerCrop()
                    .into(imageView);
        }else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.artist_name_textview);
        textView.setText(artist.getName());

        return convertView;
    }

    public List<SpotifyArtist> getArtists(){
        return mArtists;
    }

    public void setArtists(List<SpotifyArtist> artists){
        addAll(artists);
    }
}
