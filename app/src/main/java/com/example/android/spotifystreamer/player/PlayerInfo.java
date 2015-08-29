package com.example.android.spotifystreamer.player;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by achiang on 8/24/15.
 */
public class PlayerInfo implements Parcelable {
    private String artist;
    private String album;
    private String track;
    private String image;
    private String duration;
    private String progress;
    private boolean toPrev;
    private boolean playing;
    private boolean toNext;

    public PlayerInfo(String artist, String album, String track, String image, String duration,
                      String progress, boolean toPrev, boolean playing, boolean toNext) {
        this.artist = artist;
        this.album = album;
        this.track = track;
        this.image = image;
        this.duration = duration;
        this.progress = progress;
        this.toPrev = toPrev;
        this.playing = playing;
        this.toNext = toNext;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTrack() {
        return track;
    }

    public String getImage() {
        return image;
    }

    public String getDuration() {
        return duration;
    }

    public String getProgress() {
        return progress;
    }

    public boolean getToPrev() {
        return toPrev;
    }

    public boolean getPlaying() {
        return playing;
    }

    public boolean getToNext() {
        return toNext;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getArtist());
        dest.writeString(getAlbum());
        dest.writeString(getTrack());
        dest.writeString(getImage());
        dest.writeString(getDuration());
        dest.writeString(getProgress());
        dest.writeByte((byte) (toPrev ? 1 : 0));
        dest.writeByte((byte) (playing ? 1 : 0));
        dest.writeByte((byte) (toNext ? 1 : 0));
    }

    public static final Parcelable.Creator<PlayerInfo> CREATOR =
            new Parcelable.Creator<PlayerInfo>() {
                @Override
                public PlayerInfo createFromParcel(Parcel source) {
                    return new PlayerInfo(source);
                }

                @Override
                public PlayerInfo[] newArray(int size) {
                    return new PlayerInfo[size];
                }
            };

    private PlayerInfo(Parcel parcel) {
        artist = parcel.readString();
        album = parcel.readString();
        track = parcel.readString();
        image = parcel.readString();
        duration = parcel.readString();
        progress = parcel.readString();
        toPrev = ( parcel.readByte() != 0 );
        playing = ( parcel.readByte() != 0 );
        toNext = ( parcel.readByte() != 0);
    }
}
