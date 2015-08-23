package com.example.android.spotifystreamer.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by achiang on 8/16/15.
 */

/**
 * Defines table and columns name for the TopTracks database.
 */
public class TopTracksContract {

    /* The following 3 constants are defined for content provider. */

    // Uses the package name as the content authority.
    public static final String CONTENT_AUTHORITY = "com.example.android.spotifystreamer";

    // Uses content authority to create the base of all URIs which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path : content://com.example.android.spotifystreamer/tracks
    public static final String PATH_TRACKS = TopTracksEntry.TABLE_NAME;



    /* Inner class that defines the contents of the top tracks table */
    public static final class TopTracksEntry implements BaseColumns {

        /* The following 3 constants are defined for content provider. */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACKS;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACKS;



        /* The following 7 constants are defined for columns of table. */
        public static final String TABLE_NAME = "tracks";

        /* Columns for the top tracks table */

        // _ID is inherited by implementing the BaseColumns.

        public static final String COLUMN_ARTIST = "artist";

        public static final String COLUMN_ALBUM = "album";

        public static final String COLUMN_TRACK = "track";

        public static final String COLUMN_SMALL_IMAGE = "small_image";

        public static final String COLUMN_LARGE_IMAGE = "large_image";

        public static final String COLUMN_PREVIEW_URL = "preview_url";

        /* The following methods are defined for content provider querying. */

        public static Uri buildTrackUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
