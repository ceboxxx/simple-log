package com.ceboxxx.simplelog;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Process;
import android.os.UserManager;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class LogStore extends SQLiteOpenHelper {
    private static final long DAY = 86400000L;
    LogStore(Context c) { super(c, "history.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries(time INTEGER NOT NULL, package TEXT NOT NULL, label TEXT NOT NULL, UNIQUE(time, package))");
        db.execSQL("CREATE INDEX entries_time ON entries(time)");
        db.execSQL("CREATE TABLE state(id INTEGER PRIMARY KEY CHECK(id=1), cursor INTEGER NOT NULL, foreground TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    static boolean hasAccess(Context c) {
        AppOpsManager ops = (AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
    @SuppressWarnings("deprecation")
    static synchronized void collect(Context c) {
        if (!hasAccess(c) || !c.getSystemService(UserManager.class).isUserUnlocked()) return;
        UsageStatsManager manager = c.getSystemService(UsageStatsManager.class);
        PackageManager pm = c.getPackageManager();
        Set<String> ignored = new HashSet<>();
        ignored.add(c.getPackageName());
        ignored.add("com.android.systemui");
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : pm.queryIntentActivities(home, 0)) ignored.add(info.activityInfo.packageName);
        try (LogStore store = new LogStore(c)) {
            SQLiteDatabase db = store.getWritableDatabase();
            long end = System.currentTimeMillis() - 2000;
            long start = end - DAY;
            String previous = null;
            try (Cursor s = db.rawQuery("SELECT cursor, foreground FROM state WHERE id=1", null)) {
                if (s.moveToFirst()) { start = s.getLong(0); previous = s.getString(1); }
            }
            if (start > end) return; // Clock moved backwards: wait rather than duplicate the history.
            // Android only retains a few days. Never claim to recover older unavailable events.
            if (start < end - 7 * DAY) { start = end - 7 * DAY; previous = null; }
            ForegroundTracker tracker = new ForegroundTracker(previous);
            while (start < end) {
                long until = Math.min(start + DAY, end);
                UsageEvents events = manager.queryEvents(start, until);
                if (events == null) return;
                db.beginTransaction();
                try {
                    UsageEvents.Event event = new UsageEvents.Event();
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event);
                        int type = event.getEventType();
                        if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE || type == UsageEvents.Event.DEVICE_SHUTDOWN || type == UsageEvents.Event.DEVICE_STARTUP) {
                            tracker.reset();
                        } else if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
                            String pkg = event.getPackageName();
                            if (tracker.resume(pkg) && !ignored.contains(pkg)) {
                                String label = pkg;
                                try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
                                catch (PackageManager.NameNotFoundException ignoredError) { }
                                db.execSQL("INSERT OR IGNORE INTO entries(time, package, label) VALUES(?,?,?)", new Object[]{event.getTimeStamp(), pkg, label});
                            }
                        }
                    }
                    db.execSQL("INSERT OR REPLACE INTO state(id,cursor,foreground) VALUES(1,?,?)", new Object[]{until, tracker.current()});
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                start = until;
            }
        }
    }
    List<String> recent() {
        List<String> lines = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        try (Cursor c = getReadableDatabase().rawQuery("SELECT time,label FROM entries ORDER BY time DESC LIMIT 1000", null)) {
            while (c.moveToNext()) lines.add(format.format(new Date(c.getLong(0))) + " - " + c.getString(1));
        }
        return lines;
    }
    void export(Writer writer) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        try (Cursor c = getReadableDatabase().rawQuery("SELECT time,label FROM entries ORDER BY time ASC", null)) {
            while (c.moveToNext()) {
                writer.write(format.format(new Date(c.getLong(0))) + " - " + c.getString(1).replace('\n', ' ').replace('\r', ' ') + "\n");
            }
        }
    }
}
