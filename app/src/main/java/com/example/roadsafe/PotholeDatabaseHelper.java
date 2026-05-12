package com.example.roadsafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PotholeDatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME      = "roadsafe.db";
    public static final int    DB_VERSION   = 7;   // ← bumped: forces onUpgrade → fresh schema

    // ── potholes table ──────────────────────────────────────────────────────
    public static final String TABLE_POTHOLES  = "potholes";
    public static final String TABLE_NAME      = TABLE_POTHOLES;   // alias for PotholeProvider
    public static final String COL_ID          = "_id";
    public static final String COL_LAT         = "latitude";
    public static final String COL_LNG         = "longitude";
    public static final String COL_SEVERITY    = "severity";
    public static final String COL_IMPACT      = "impactScore";
    public static final String COL_STATUS      = "status";
    public static final String COL_TIMESTAMP   = "timestamp";
    public static final String COL_USERNAME    = "username";

    // ── users / leaderboard table ────────────────────────────────────────────
    public static final String TABLE_USERS     = "users";
    public static final String COL_USER_NAME   = "username";
    public static final String COL_POINTS      = "points";

    // ── point values ────────────────────────────────────────────────────────
    public static final int POINTS_UPLOAD      = 10;
    public static final int POINTS_RESOLVE     = 5;

    public PotholeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── Schema ───────────────────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_POTHOLES + " (" +
                COL_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LAT       + " REAL, " +
                COL_LNG       + " REAL, " +
                COL_SEVERITY  + " TEXT, " +
                COL_IMPACT    + " INTEGER, " +
                COL_STATUS    + " TEXT DEFAULT 'Pending', " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_USERNAME  + " TEXT DEFAULT 'Guest')");

        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_NAME + " TEXT PRIMARY KEY, " +
                COL_POINTS    + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // For older schemas that are missing columns, safest is a full rebuild.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POTHOLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
    public int getUserCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }
    // ── Points helpers ───────────────────────────────────────────────────────

    public void addPoints(String username, int points) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(
            "INSERT OR IGNORE INTO " + TABLE_USERS +
            " (" + COL_USER_NAME + ", " + COL_POINTS + ") VALUES (?, 0)",
            new Object[]{username});
        db.execSQL(
            "UPDATE " + TABLE_USERS +
            " SET " + COL_POINTS + " = " + COL_POINTS + " + ? " +
            "WHERE " + COL_USER_NAME + " = ?",
            new Object[]{points, username});
    }

    public Cursor getLeaderboard() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT " + COL_USER_NAME + ", " + COL_POINTS +
            " FROM " + TABLE_USERS +
            " ORDER BY " + COL_POINTS + " DESC",
            null);
    }

    public int getPoints(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT " + COL_POINTS + " FROM " + TABLE_USERS +
            " WHERE " + COL_USER_NAME + " = ?",
            new String[]{username});
        int pts = 0;
        if (c != null) {
            if (c.moveToFirst()) pts = c.getInt(0);
            c.close();
        }
        return pts;
    }

    // ── Status helper ────────────────────────────────────────────────────────

    public boolean updateStatus(int id, String newStatus, String username) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS, newStatus);
        int rows = db.update(TABLE_POTHOLES, cv,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0 && "Resolved".equals(newStatus)) {
            addPoints(username, POINTS_RESOLVE);
        }
        return rows > 0;
    }

    // ── Chart data helper ────────────────────────────────────────────────────

    public Cursor getReportsPerDay() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT strftime('%d/%m', datetime(" + COL_TIMESTAMP + "/1000,'unixepoch')) AS day, " +
            "COUNT(*) AS cnt " +
            "FROM " + TABLE_POTHOLES + " " +
            "WHERE " + COL_TIMESTAMP + " >= strftime('%s','now','-6 days') * 1000 " +
            "GROUP BY day ORDER BY " + COL_TIMESTAMP + " ASC",
            null);
    }

    // ── Count by status ─────────────────────────────────────────────────────

    public int getCountByStatus(String status) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_POTHOLES +
            " WHERE " + COL_STATUS + " = ?",
            new String[]{status});
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int getResolvedCount() { return getCountByStatus("Resolved"); }
    public int getPendingCount()  { return getCountByStatus("Pending"); }

    // ── Nearby potholes (for duplicate detection) ───────────────────────────

    public Cursor getPotholesNearby(double lat, double lng, double radiusMeters) {
        // Approximate degree offset for given radius
        double degOffset = radiusMeters / 111320.0;
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT * FROM " + TABLE_POTHOLES +
            " WHERE " + COL_LAT + " BETWEEN ? AND ?" +
            " AND " + COL_LNG + " BETWEEN ? AND ?",
            new String[]{
                String.valueOf(lat - degOffset),
                String.valueOf(lat + degOffset),
                String.valueOf(lng - degOffset),
                String.valueOf(lng + degOffset)
            });
    }

    // ── Most common severity ────────────────────────────────────────────────

    public String getMostCommonSeverity() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT " + COL_SEVERITY + ", COUNT(*) AS cnt FROM " + TABLE_POTHOLES +
            " GROUP BY " + COL_SEVERITY + " ORDER BY cnt DESC LIMIT 1",
            null);
        String result = "N/A";
        if (c != null) {
            if (c.moveToFirst()) result = c.getString(0);
            c.close();
        }
        return result;
    }

    // ── Weekly trend ────────────────────────────────────────────────────────

    public int getThisWeekCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_POTHOLES +
            " WHERE " + COL_TIMESTAMP + " >= strftime('%s','now','-6 days') * 1000",
            null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int getLastWeekCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_POTHOLES +
            " WHERE " + COL_TIMESTAMP + " >= strftime('%s','now','-13 days') * 1000" +
            " AND " + COL_TIMESTAMP + " < strftime('%s','now','-6 days') * 1000",
            null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }

    // ── Get username for a pothole (for reporter restriction) ───────────────

    public String getUsernameForPothole(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT " + COL_USERNAME + " FROM " + TABLE_POTHOLES +
            " WHERE " + COL_ID + " = ?",
            new String[]{String.valueOf(id)});
        String username = null;
        if (c != null) {
            if (c.moveToFirst()) username = c.getString(0);
            c.close();
        }
        return username;
    }
}
