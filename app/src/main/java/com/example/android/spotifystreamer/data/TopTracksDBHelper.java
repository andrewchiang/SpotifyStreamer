package com.example.android.spotifystreamer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.spotifystreamer.data.TopTracksContract.TopTracksEntry;

/**
 * Created by achiang on 8/16/15.
 */

/**
 * Manages a local data for top tracks.
 */
public class TopTracksDBHelper extends SQLiteOpenHelper {

    // Manually increment version is required if the database scheme changed.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "tracks.db";

    public TopTracksDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_TOP_TRACKS_TABLE = "CREATE TABLE " + TopTracksEntry.TABLE_NAME
                + " ("
                + TopTracksEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TopTracksEntry.COLUMN_ARTIST + " TEXT NOT NULL, "
                + TopTracksEntry.COLUMN_ALBUM + " TEXT NOT NULL, "
                + TopTracksEntry.COLUMN_TRACK + " TEXT NOT NULL, "
                + TopTracksEntry.COLUMN_SMALL_IMAGE + " TEXT NOT NULL, "
                + TopTracksEntry.COLUMN_LARGE_IMAGE + " TEXT NOT NULL, "
                + TopTracksEntry.COLUMN_PREVIEW_URL + " TEXT NOT NULL "
                + " );";

        db.execSQL(SQL_CREATE_TOP_TRACKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TopTracksEntry.TABLE_NAME);
        onCreate(db);
    }
}
