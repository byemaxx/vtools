package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.FpsWatchSession;

import java.util.ArrayList;

public class FpsWatchStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public FpsWatchStore(Context context) {
        super(context, "fps_watch_log2", null, DB_VERSION);
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table session(" +
                    "id INTEGER primary key, " +
                    "package_name text," +
                    "time_begin INTEGER default(-1)," +
                    "time_end INTEGER default(-1)" +
                    ")");
            db.execSQL("create table fps_history(" +
                    "id INTEGER primary key AUTOINCREMENT," +
                    "time INTEGER," +
                    "session INTEGER," +
                    "fps REAL," +
                    "cpu_load REAL," +
                    "gpu_load REAL," +
                    "capacity INTEGER," +
                    "temperature REAL," +
                    "power_mode text" +
                    ")");
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // 列举会话
    public synchronized ArrayList<FpsWatchSession> sessions() {
        ArrayList<FpsWatchSession> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select * from session", new String[]{})) {
            while (cursor.moveToNext()) {
                histories.add(new FpsWatchSession() {{
                    sessionId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    packageName = cursor.getString(cursor.getColumnIndexOrThrow("package_name"));
                    beginTime = cursor.getLong(cursor.getColumnIndexOrThrow("time_begin"));
                }});
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话的详情（帧率）
    public synchronized ArrayList<Float> sessionFpsData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select fps from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话的详情（温度）
    public synchronized ArrayList<Float> sessionTemperatureData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select temperature from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话的详情（CPU 负载）
    public synchronized ArrayList<Float> sessionCpuLoadData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select cpu_load from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话的详情（GPU 负载）
    public synchronized ArrayList<Float> sessionGpuLoadData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select gpu_load from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话的详情（电量）
    public synchronized ArrayList<Float> sessionCapacityData(long sessionId) {
        ArrayList<Float> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select capacity from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                histories.add(cursor.getFloat(0));
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return histories;
    }

    // 获取会话中的平静帧率
    public synchronized float sessionAvgFps(long sessionId) {
        float result = 0;
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select avg(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return result;
    }

    // 获取会话中的最低帧率
    public synchronized float sessionMinFps(long sessionId) {
        float result = 0;
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select min(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return result;
    }

    // 获取会话中的最高帧率
    public synchronized float sessionMaxFps(long sessionId) {
        float result = 0;
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select max(fps) from fps_history where session = ?", new String[]{
                    "" + sessionId
            })) {
            while (cursor.moveToNext()) {
                result = cursor.getFloat(0);
            }
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database operation failed", error);
        }
        return result;
    }

    // 创建会话
    public synchronized long createSession(String packageName) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        long time = System.currentTimeMillis();
        try {
            database.execSQL("insert into session(id, package_name, time_begin) values (?, ?, ?)", new Object[]{
                    time,
                    packageName,
                    time
            });
            database.setTransactionSuccessful();
            return time;
        } catch (Exception ex) {
            return -1;
        } finally {
            database.endTransaction();
        }
    }

    // 添加记录
    public synchronized boolean addHistory(long session, float fps, double cpuLoad, double gpuLoad, int capacity, double temperature, String powerMode) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into fps_history(time, session, fps, cpu_load, gpu_load, capacity, temperature, power_mode) values (?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{
                System.currentTimeMillis(),
                session,
                fps,
                cpuLoad,
                gpuLoad,
                capacity,
                temperature,
                powerMode
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            android.util.Log.e("FpsWatchStore", "Database write failed", ex);
            return false;
        } finally {
            database.endTransaction();
        }
    }

    // Delete both tables atomically, preserving the database if either statement fails.
    public synchronized boolean clearAll() {
        return deleteHistory(null);
    }

    public synchronized boolean deleteSession(long sessionId) {
        return deleteHistory(sessionId);
    }

    private boolean deleteHistory(Long sessionId) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            if (sessionId == null) {
                database.execSQL("delete from session");
                database.execSQL("delete from fps_history");
            } else {
                database.execSQL("delete from session where id = ?", new Object[]{sessionId});
                database.execSQL("delete from fps_history where session = ?", new Object[]{sessionId});
            }
            database.setTransactionSuccessful();
            return true;
        } catch (Exception error) {
            android.util.Log.e("FpsWatchStore", "Database delete failed", error);
            return false;
        } finally {
            database.endTransaction();
        }
    }
}
