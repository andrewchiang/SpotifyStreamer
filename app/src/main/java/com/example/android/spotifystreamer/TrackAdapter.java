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
public class TrackAdapter extends ArrayAdapter<SpotifyTrack> {
    private List<SpotifyTrack> mTracks;

    public TrackAdapter(Context context, ArrayList<SpotifyTrack> tracks) {
        super(context, 0, tracks);
        mTracks = tracks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SpotifyTrack track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.track_thumbnail_imageview);

        if (track.getSmall_image_url() != "") {
            Picasso.with(getContext())
                    .load(track.getSmall_image_url())
                    .resizeDimen(56,56)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
        }

        ((TextView) convertView.findViewById(R.id.track_name_textview)).setText(track.getTrack_name());
        ((TextView) convertView.findViewById(R.id.album_name_textview)).setText(track.getAlbum_name());

        return convertView;
    }

    public List<SpotifyTrack> getTracks() {
        return mTracks;
    }

    public void setTracks(List<SpotifyTrack> tracks) {
        addAll(tracks);
    }
}
