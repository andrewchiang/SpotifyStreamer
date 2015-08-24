package com.example.android.spotifystreamer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import com.example.android.spotifystreamer.data.TopTracksDBHelper;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by achiang on 6/29/15.
 */
public final class Utils {
    private Utils(){}

    public static void displayToast(Context context, int resId, int duration, int gravity){
        Toast toast = Toast.makeText(context,resId,duration);
        toast.setGravity(gravity,0,0);
        toast.show();
    }

    public static String findImageForListItem(List<Image> images){
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
     * @param context
     * @param tableName
     */
    private static void resetTableColumnId(Context context, String tableName){
        SQLiteDatabase db = new TopTracksDBHelper(context).getWritableDatabase();
        db.execSQL("delete from sqlite_sequence where name='" + tableName + "'");
        db.close();
    }

    /**
     * This method is used for deleting all existing records of a table
     * , and it also reset the _ID column after deletion.
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
}
