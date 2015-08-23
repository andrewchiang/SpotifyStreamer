package com.example.android.spotifystreamer.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by achiang on 8/17/15.
 */
public class TopTracksProvider extends ContentProvider {

    private TopTracksDBHelper mDBHelper;

    private static final UriMatcher sUriMatcher = buildMatcher();

    static final int TRACKS = 100;
    static final int TRACKS_WITH_ID = 101;

    static UriMatcher buildMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = TopTracksContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, TopTracksContract.PATH_TRACKS, TRACKS);
        matcher.addURI(authority, TopTracksContract.PATH_TRACKS + "/#", TRACKS_WITH_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDBHelper = new TopTracksDBHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case TRACKS:
                return TopTracksContract.TopTracksEntry.CONTENT_TYPE;
            case TRACKS_WITH_ID:
                return TopTracksContract.TopTracksEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {
            case TRACKS:
                retCursor = mDBHelper.getReadableDatabase().query(
                        TopTracksContract.TopTracksEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        "_ID ASC"
                );
                break;
            case TRACKS_WITH_ID:
                selection = "_ID = " + uri.getLastPathSegment();

                retCursor = mDBHelper.getReadableDatabase().query(
                        TopTracksContract.TopTracksEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Uri: " + uri);
        }

        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri retUri;

        switch (match) {
            case TRACKS:
                long _id = db.insert(TopTracksContract.TopTracksEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    retUri = TopTracksContract.TopTracksEntry.buildTrackUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        return retUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted = 0;

        if (selection == null) {
            selection = "1";
        }

        switch (match) {
            case TRACKS:
                rowsDeleted = db.delete(TopTracksContract.TopTracksEntry.TABLE_NAME,
                        selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch(match){
            case TRACKS:
                db.beginTransaction();
                int retCount = 0;

                try{

                    for(ContentValues value : values){
                        long _id = db.insert(
                                TopTracksContract.TopTracksEntry.TABLE_NAME,
                                null,
                                value
                        );

                        if(_id != -1){
                            retCount++;
                        }
                    }

                    db.setTransactionSuccessful();
                }finally {
                    db.endTransaction();
                }

                getContext().getContentResolver().notifyChange(uri, null);
                return retCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
