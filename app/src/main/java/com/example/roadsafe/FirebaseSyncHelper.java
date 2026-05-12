package com.example.roadsafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseSyncHelper {

    private final Context context;
    private final PotholeDatabaseHelper dbHelper;
    private final FirebaseFirestore db;

    public FirebaseSyncHelper(Context context) {
        this.context  = context;
        this.dbHelper = new PotholeDatabaseHelper(context);
        this.db       = FirebaseFirestore.getInstance();
    }

    // ── Call this from HomeActivity.onResume() ────────────────────────────────
    public void syncFromFirebase(Runnable onComplete) {
        db.collection("potholes").get()
                .addOnSuccessListener(query -> {
                    SQLiteDatabase sqLiteDB = dbHelper.getWritableDatabase();

                    for (var doc : query) {
                        Double lat    = doc.getDouble("latitude");
                        Double lng    = doc.getDouble("longitude");
                        String sev    = doc.getString("severity");
                        String status = doc.getString("status");
                        String path   = doc.getString("imagePath");
                        String user   = doc.getString("username");
                        Long   ts     = doc.getLong("timestamp");

                        if (lat == null || lng == null) continue;
                        if (status == null && ts == null) continue;
                        // Deduplicate — match by lat + lng + timestamp
                        Cursor existing = sqLiteDB.rawQuery(
                                "SELECT _id FROM potholes " +
                                        "WHERE latitude=? AND longitude=? AND timestamp=?",
                                new String[]{
                                        String.valueOf(lat),
                                        String.valueOf(lng),
                                        String.valueOf(ts != null ? ts : 0)
                                });

                        boolean alreadyExists = existing != null
                                && existing.getCount() > 0;
                        if (existing != null) existing.close();

                        if (!alreadyExists) {
                            ContentValues cv = new ContentValues();
                            cv.put("latitude",    lat);
                            cv.put("longitude",   lng);
                            cv.put("severity",    sev    != null ? sev    : "Low");
                            cv.put("status",      status != null ? status : "Pending");
                            cv.put("timestamp",   ts     != null ? ts     : 0);
                            cv.put("username",    user   != null ? user   : "Guest");
                            cv.put("impactScore", 0);
                            sqLiteDB.insert(
                                    PotholeDatabaseHelper.TABLE_POTHOLES, null, cv);

                            // Award points to the original reporter
                            if (user != null && !user.isEmpty()) {
                                dbHelper.addPoints(
                                        user, PotholeDatabaseHelper.POINTS_UPLOAD);
                            }
                        }
                    }

                    // Notify caller (e.g. HomeActivity to refresh dashboard)
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    // Offline — silently skip, SQLite still has local data
                    if (onComplete != null) onComplete.run();
                });
    }
}