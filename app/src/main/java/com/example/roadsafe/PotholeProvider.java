package com.example.roadsafe;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class PotholeProvider extends ContentProvider {

    public static final String AUTHORITY  = "com.example.roadsafe.provider";
    public static final Uri    CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/potholes");

    // Match /potholes        → list
    // Match /potholes/{id}   → single row  ← was missing before
    private static final int POTHOLES    = 1;
    private static final int POTHOLE_ID  = 2;

    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(AUTHORITY, "potholes",    POTHOLES);
        matcher.addURI(AUTHORITY, "potholes/#",  POTHOLE_ID);   // ← added
    }

    PotholeDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new PotholeDatabaseHelper(getContext());
        return true;
    }

    // ── INSERT ──────────────────────────────────────────────────────────────
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(PotholeDatabaseHelper.TABLE_NAME, null, values);
        if (id == -1) return null;

        Uri newUri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    // ── QUERY ───────────────────────────────────────────────────────────────
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (matcher.match(uri) == POTHOLE_ID) {
            // Narrow query to the single row whose _id is in the URI
            selection     = PotholeDatabaseHelper.COL_ID + " = ?";
            selectionArgs = new String[]{ uri.getLastPathSegment() };
        }

        Cursor c = db.query(PotholeDatabaseHelper.TABLE_NAME,
                projection, selection, selectionArgs, null, null, sortOrder);
        if (c != null) c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    // ── UPDATE ──────────────────────────────────────────────────────────────
    // This was the broken method — it now actually writes to SQLite.
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected;

        switch (matcher.match(uri)) {
            case POTHOLE_ID:
                // /potholes/42  → update only row with _id = 42
                String id = uri.getLastPathSegment();
                rowsAffected = db.update(
                        PotholeDatabaseHelper.TABLE_NAME,
                        values,
                        PotholeDatabaseHelper.COL_ID + " = ?",
                        new String[]{ id }
                );
                break;

            case POTHOLES:
                // /potholes  → bulk update with caller-supplied WHERE clause
                rowsAffected = db.update(
                        PotholeDatabaseHelper.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsAffected > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsAffected;
    }

    // ── DELETE ──────────────────────────────────────────────────────────────
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        if (matcher.match(uri) == POTHOLE_ID) {
            rowsDeleted = db.delete(
                    PotholeDatabaseHelper.TABLE_NAME,
                    PotholeDatabaseHelper.COL_ID + " = ?",
                    new String[]{ uri.getLastPathSegment() }
            );
        } else {
            rowsDeleted = db.delete(
                    PotholeDatabaseHelper.TABLE_NAME, selection, selectionArgs);
        }

        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) { return null; }
}
