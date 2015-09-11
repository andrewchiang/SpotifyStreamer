package com.example.android.spotifystreamer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import com.example.android.spotifystreamer.data.TopTracksContract;
import com.example.android.spotifystreamer.data.TopTracksDBHelper;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by achiang on 6/29/15.
 */
public final class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    public static void displayToast(Context context, int resId, int duration, int gravity) {
        Toast toast = Toast.makeText(context, resId, duration);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }

    public static String findImageForListItem(List<Image> images) {
        Image thumbnail = images.get(0);
        for (int i = 1; i < images.size(); ++i) {
            Image tmp = images.get(i);
            if (tmp.width < thumbnail.width && tmp.width >= 200) {
                thumbnail = tmp;
            }
        }
        return thumbnail.url;
    }

    /**
     * This method will be used for resetting the _ID column of a table in the database,
     * so it will always be starting and incrementing from 0.
     *
     * @param context
     * @param tableName
     */
    private static void resetTableColumnId(Context context, String tableName) {
        SQLiteDatabase db = new TopTracksDBHelper(context).getWritableDatabase();
        db.execSQL("delete from sqlite_sequence where name='" + tableName + "'");
        db.close();
    }

    /**
     * This method is used for deleting all existing records of a table
     * , and it also reset the _ID column after deletion.
     *
     * @param context
     * @param uri
     * @param tableName
     * @return The count of deleted rows.
     */
    public static int deleteTracksFromDb(Context context, Uri uri, String tableName) {
        int rowsDeleted = context.getContentResolver().delete(
                uri,
                null,
                null
        );

        // Executes this function to reset the column _ID,
        resetTableColumnId(context, tableName);

        return rowsDeleted;
    }

    /**
     * Retrieves all tracks from the table 'tracks' in database and return to calling method.
     */
    public static List<SpotifyTrack> getTracksFromDb(Context context) {
        List<SpotifyTrack> tracks = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                TopTracksContract.TopTracksEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String artist = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_ALBUM));
                String track_name = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_TRACK));
                String small_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_SMALL_IMAGE));
                String large_artwork = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_LARGE_IMAGE));
                String preview_url = cursor.getString(cursor.getColumnIndex(TopTracksContract.TopTracksEntry.COLUMN_PREVIEW_URL));

                SpotifyTrack track = new SpotifyTrack(
                        artist,
                        track_name,
                        album,
                        small_artwork,
                        large_artwork,
                        preview_url
                );

                tracks.add(track);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return tracks;
    }
}
