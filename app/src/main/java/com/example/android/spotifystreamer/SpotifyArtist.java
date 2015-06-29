package com.example.android.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by achiang on 6/29/15.
 */
public class SpotifyArtist implements Parcelable {
    private String id;
    private String name;
    private String image_url;

    public SpotifyArtist(String id, String name, String image_url) {
        this.id = id;
        this.name = name;
        this.image_url = image_url;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage_url() {
        return image_url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(image_url);
    }

    public static final Parcelable.Creator<SpotifyArtist> CREATOR =
            new Parcelable.Creator<SpotifyArtist>() {
                @Override
                public SpotifyArtist createFromParcel(Parcel source) {
                    return new SpotifyArtist(source);
                }

                @Override
                public SpotifyArtist[] newArray(int size) {
                    return new SpotifyArtist[size];
                }
            };

    private SpotifyArtist(Parcel parcel) {
        id = parcel.readString();
        name = parcel.readString();
        image_url = parcel.readString();
    }
}
