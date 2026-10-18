package com.omarea.store

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.BatteryManager
import android.os.SystemClock
import com.omarea.data.GlobalStatus
import com.omarea.utils.BatteryPowerMath

class TelemetryStore(context: Context) : SQLiteOpenHelper(context.applicationContext ?: context, "monitor_telemetry", null, 1) {
    data class Session(val id: Long, val kind: String, val label: String, val started: Long, val targetFps: Int)
    data class Sample(val elapsed: Long, val time: Long, val raw: Long, val divisor: Int,
        val voltage: Double, val factor: Float, val watts: Double, val temperature: Double,
        val fps: Double, val cpu: Double, val gpu: Double, val source: String, val gap: Boolean)
    data class Frame(val elapsed: Long, val duration: Double)
    data class FrameStats(val count: Long, val maximum: Double, val longCount: Long, val veryLongCount: Long)
    private val preferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val starts = HashMap<Long, Long>()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY, kind TEXT NOT NULL, label TEXT NOT NULL, started INTEGER NOT NULL, target_fps INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE samples(session INTEGER NOT NULL REFERENCES sessions(id), elapsed INTEGER NOT NULL, time INTEGER NOT NULL, raw INTEGER, divisor INTEGER, voltage REAL, factor REAL, watts REAL, temperature REAL, fps REAL, cpu REAL, gpu REAL, source TEXT, gap INTEGER)")
        db.execSQL("CREATE INDEX sample_session ON samples(session, elapsed)")
        db.execSQL("CREATE TABLE frames(session INTEGER NOT NULL REFERENCES sessions(id), elapsed INTEGER NOT NULL, duration REAL NOT NULL)")
        db.execSQL("CREATE INDEX frame_session ON frames(session, elapsed)")
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { error("Unsupported telemetry schema: $old → $new") }

    @Synchronized fun start(kind: String, label: String): Long {
        val values = ContentValues().apply {
            put("kind", kind); put("label", label); put("started", System.currentTimeMillis())
            put("target_fps", preferences.getInt("monitor_target_fps", 60).coerceIn(15, 240))
        }
        return writableDatabase.insertOrThrow("sessions", null, values).also { starts[it] = SystemClock.elapsedRealtime() }
    }

    @Synchronized fun end(id: Long) { starts.remove(id) }

    @Synchronized fun record(id: Long, fps: Double = Double.NaN, cpu: Double = Double.NaN,
        gpu: Double = Double.NaN, durations: List<Double> = emptyList(), source: String = "", gap: Boolean = false,
        endTimes: List<Long> = emptyList()) {
        val start = starts[id] ?: return
        val elapsed = SystemClock.elapsedRealtime() - start
        val nowNs = System.nanoTime()
        val raw = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val divisor = preferences.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)
        val factor = preferences.getFloat(SpfConfig.GLOBAL_SPF_POWER_FACTOR, 1f)
        val voltage = GlobalStatus.batteryVoltage
        val power = BatteryPowerMath.watts(BatteryPowerMath.currentMa(raw, divisor), voltage, factor)
        val values = ContentValues().apply {
            put("session", id); put("elapsed", elapsed); put("time", System.currentTimeMillis())
            if (raw != Long.MIN_VALUE && raw != Long.MAX_VALUE) put("raw", raw)
            put("divisor", divisor); putFinite("voltage", voltage.takeIf { it > 0 } ?: Double.NaN)
            put("factor", factor); putFinite("watts", power)
            putFinite("temperature", GlobalStatus.temperatureCurrent.takeIf { it != -1.0 } ?: Double.NaN)
            putFinite("fps", fps.takeIf { it >= 0 } ?: Double.NaN)
            putFinite("cpu", cpu.takeIf { it >= 0 } ?: Double.NaN)
            putFinite("gpu", gpu.takeIf { it >= 0 } ?: Double.NaN)
            put("source", source); put("gap", if (gap) 1 else 0)
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertOrThrow("samples", null, values)
            for ((index, duration) in durations.withIndex()) {
                if (!duration.isFinite() || duration <= 0) continue
                val end = endTimes.getOrNull(index) ?: continue
                val offset = elapsed - (nowNs - end) / 1000000
                if (offset >= 0 && offset <= elapsed) db.execSQL("INSERT INTO frames VALUES (?, ?, ?)", arrayOf<Any>(id, offset, duration))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun latestSessionId(kind: String): Long? = readableDatabase.rawQuery(
        "SELECT id FROM sessions WHERE kind=? ORDER BY id DESC LIMIT 1", arrayOf(kind)).use { c ->
        if(c.moveToFirst()) c.getLong(0) else null
    }
    fun sessions(kind: String): List<Session> = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE kind=? ORDER BY id DESC", arrayOf(kind)).use { c ->
        buildList { while (c.moveToNext()) add(Session(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4))) }
    }
    fun samples(id: Long): List<Sample> = readableDatabase.rawQuery(
        "SELECT elapsed,time,raw,divisor,voltage,factor,watts,temperature,fps,cpu,gpu,source,gap FROM samples WHERE session=? ORDER BY elapsed", arrayOf(id.toString())).use { c ->
        fun number(i: Int) = if (c.isNull(i)) Double.NaN else c.getDouble(i)
        buildList { while(c.moveToNext()) add(Sample(c.getLong(0), c.getLong(1), if(c.isNull(2)) Long.MIN_VALUE else c.getLong(2),
            c.getInt(3), number(4), c.getFloat(5), number(6), number(7), number(8), number(9), number(10), c.getString(11), c.getInt(12) != 0)) }
    }
    fun framePeaks(id: Long): List<Frame> = readableDatabase.rawQuery(
        "SELECT elapsed/100*100,max(duration) FROM frames WHERE session=? GROUP BY elapsed/100 ORDER BY elapsed/100", arrayOf(id.toString())).use { c ->
        buildList { while(c.moveToNext()) add(Frame(c.getLong(0), c.getDouble(1))) }
    }
    fun forEachFrame(id: Long, action: (Frame) -> Unit) {
        readableDatabase.rawQuery("SELECT elapsed,duration FROM frames WHERE session=? ORDER BY elapsed", arrayOf(id.toString())).use { c ->
            while(c.moveToNext()) action(Frame(c.getLong(0), c.getDouble(1)))
        }
    }
    fun frameStats(id: Long, targetFps: Int): FrameStats = readableDatabase.rawQuery(
        "SELECT count(*),max(duration),sum(duration>?),sum(duration>?) FROM frames WHERE session=?",
        arrayOf((3000.0 / targetFps).toString(), (6000.0 / targetFps).toString(), id.toString())).use { c ->
        c.moveToFirst()
        FrameStats(c.getLong(0), if(c.isNull(1)) Double.NaN else c.getDouble(1), c.getLong(2), c.getLong(3))
    }
    private fun ContentValues.putFinite(key: String, value: Double) { if(value.isFinite()) put(key, value) else putNull(key) }
}
