package com.omarea.store;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.omarea.model.ChargeSpeedHistory;
import com.omarea.model.ChargeTimeHistory;

import java.util.ArrayList;

public class ChargeSpeedStore extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public ChargeSpeedStore(Context context) {
        super(context, "charge_history2", null, DB_VERSION);
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table charge_history(" +
                    "id INTEGER primary key AUTOINCREMENT, " +
                    "time INTEGER, " +
                    "io INTEGER, " +
                    "capacity INTEGER, " +
                    "temperature REAL" +
                    ")");
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Database operation failed", error);
        }
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // 获取总充入电量
    public synchronized int getSum() {
        int total = 0;
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select sum(io) as total from charge_history", new String[]{})) {
            while (cursor.moveToNext()) {
                total = (int) (cursor.getLong(cursor.getColumnIndexOrThrow("total")) / 3600);
            }
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Database operation failed", error);
        }
        return total;
    }

    public synchronized ArrayList<ChargeSpeedHistory> statistics() {
        ArrayList<ChargeSpeedHistory> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select capacity, avg(io) as io from charge_history group by capacity order by capacity", new String[]{})) {
            while (cursor.moveToNext()) {
                histories.add(new ChargeSpeedHistory() {{
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity"));
                    io = cursor.getLong(cursor.getColumnIndexOrThrow("io"));
                }});
            }
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Database operation failed", error);
        }
        return histories;
    }

    public synchronized ArrayList<ChargeSpeedHistory> getTemperature() {
        ArrayList<ChargeSpeedHistory> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select capacity, max(temperature) as temperature from charge_history group by capacity", new String[]{})) {
            while (cursor.moveToNext()) {
                histories.add(new ChargeSpeedHistory() {{
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity"));
                    temperature = cursor.getFloat(cursor.getColumnIndexOrThrow("temperature"));
                }});
            }
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Database operation failed", error);
        }
        return histories;
    }

    public synchronized ArrayList<ChargeTimeHistory> chargeTime() {
        ArrayList<ChargeTimeHistory> histories = new ArrayList<>();
        try (SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
             Cursor cursor = sqLiteDatabase.rawQuery("select capacity, min(time) as start_time, max(time) as end_time from charge_history group by capacity", new String[]{})) {
            while (cursor.moveToNext()) {
                histories.add(new ChargeTimeHistory() {{
                    startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));
                    endTime = cursor.getLong(cursor.getColumnIndexOrThrow("end_time"));
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity"));
                }});
            }
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Database operation failed", error);
        }
        return histories;
    }

    public synchronized boolean addHistory(long io, int capacity, double temperature) {
        SQLiteDatabase database = getWritableDatabase();
        getWritableDatabase().beginTransaction();
        try {
            database.execSQL("insert into charge_history(time, io, capacity, temperature) values (?, ?, ?, ?)", new Object[]{
                    System.currentTimeMillis(),
                    io,
                    capacity,
                    temperature
            });
            database.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            android.util.Log.e("ChargeSpeedStore", "Database write failed", ex);
            return false;
        } finally {
            database.endTransaction();
        }
    }

    public synchronized int lastCapacity() {
        try (SQLiteDatabase database = getReadableDatabase();
             Cursor cursor = database.rawQuery("select max(capacity) AS capacity from charge_history", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Exception error) {
            android.util.Log.e("ChargeSpeedStore", "Unable to read last charging capacity", error);
            return 0;
        }
    }

    public synchronized boolean clearAll() {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from  charge_history", new String[]{});
            return true;
        } catch (Exception ex) {
            android.util.Log.e("ChargeSpeedStore", "Database write failed", ex);
            return false;
        }
    }

    public synchronized boolean handleConflics(int capacity) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            database.execSQL("delete from charge_history where capacity >= ?", new Object[]{
                    capacity
            });
            return true;
        } catch (Exception ex) {
            android.util.Log.e("ChargeSpeedStore", "Database write failed", ex);
            return false;
        }
    }
}

