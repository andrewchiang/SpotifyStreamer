package com.example.android.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by achiang on 6/29/15.
 */
public class SpotifyTrack implements Parcelable {
    private String artist_name;
    private String track_name;
    private String album_name;
    private String small_image_url; // image for list item
    private String large_image_url; // image for streaming audio in stage 2.
    private String preview_url; // This is used for streaming audio in stage 2.

    public SpotifyTrack(String artist_name, String track_name, String album_name, String small_image_url,
                        String large_image_url, String preview_url) {
        this.artist_name = artist_name;
        this.track_name = track_name;
        this.album_name = album_name;
        this.small_image_url = small_image_url;
        this.large_image_url = large_image_url;
        this.preview_url = preview_url;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public String getTrack_name() {
        return track_name;
    }

    public String getAlbum_name() {
        return album_name;
    }

    public String getSmall_image_url() {
        return small_image_url;
    }

    public String getLarge_image_url() {
        return large_image_url;
    }

    public String getPreview_url() {
        return preview_url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(artist_name);
        dest.writeString(track_name);
        dest.writeString(album_name);
        dest.writeString(small_image_url);
        dest.writeString(large_image_url);
        dest.writeString(preview_url);
    }

    public static final Parcelable.Creator<SpotifyTrack> CREATOR =
            new Parcelable.Creator<SpotifyTrack>() {
                @Override
                public SpotifyTrack createFromParcel(Parcel source) {
                    return new SpotifyTrack(source);
                }

                @Override
                public SpotifyTrack[] newArray(int size) {
                    return new SpotifyTrack[size];
                }
            };

    private SpotifyTrack(Parcel parcel) {
        artist_name = parcel.readString();
        track_name = parcel.readString();
        album_name = parcel.readString();
        small_image_url = parcel.readString();
        large_image_url = parcel.readString();
        preview_url = parcel.readString();
    }
}
