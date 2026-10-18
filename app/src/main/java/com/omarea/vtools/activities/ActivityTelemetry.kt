package com.omarea.vtools.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.omarea.store.TelemetryStore
import com.omarea.ui.TelemetryChart
import com.omarea.utils.TelemetryMath
import com.omarea.vtools.R
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityTelemetry : ActivityBase() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var store: TelemetryStore
    private lateinit var primary: Spinner
    private lateinit var secondary: Spinner
    private lateinit var metric: Spinner
    private lateinit var chart: TelemetryChart
    private lateinit var summary: TextView
    private var sessions = emptyList<TelemetryStore.Session>()
    private var refresh: Job? = null
    private var exportIds = longArrayOf()
    private var active = false
    private data class Report(val session: TelemetryStore.Session, val samples: List<TelemetryStore.Sample>,
        val frames: List<TelemetryStore.Frame>, val stats: TelemetryStore.FrameStats)
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val output = contentResolver.openOutputStream(uri) ?: error("Unable to open destination")
                    output.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.appendLine("record_type,session,label,started_ms,target_fps,elapsed_ms,time_ms,raw_current,divisor,voltage_v,factor,power_w,temperature_c,fps,cpu_percent,gpu_percent,frame_ms,frame_source,frame_gap")
                        val exportSessions = store.sessions(intent.getStringExtra("kind") ?: "game")
                        for (id in exportIds) {
                            val session = exportSessions.firstOrNull { it.id == id } ?: error("Session $id is unavailable")
                            fun row(values: List<Any?>) {
                                writer.appendLine(values.joinToString(",") { value ->
                                    val text = if (value is Double && !value.isFinite()) "" else value?.toString() ?: ""
                                    "\"" + text.replace("\"", "\"\"") + "\""
                                })
                            }
                            for (s in store.samples(id)) row(listOf("sample", id, session.label, session.started, session.targetFps,
                                s.elapsed, s.time, s.raw.takeUnless { it == Long.MIN_VALUE }, s.divisor, s.voltage, s.factor,
                                s.watts, s.temperature, s.fps, s.cpu, s.gpu, null, s.source, s.gap))
                            store.forEachFrame(id) { f -> row(listOf("frame", id, session.label, session.started, session.targetFps,
                                f.elapsed, null, null, null, null, null, null, null, null, null, null, f.duration, null, null)) }
                        }
                    }
                }
            }.onSuccess { Toast.makeText(this@ActivityTelemetry, "CSV exported", Toast.LENGTH_SHORT).show() }
                .onFailure { if(it !is CancellationException) Toast.makeText(this@ActivityTelemetry, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telemetry)
        setBackArrow()
        title = if (intent.getStringExtra("kind") == "charge") "Charging power history" else "FPS / power sessions"
        store = TelemetryStore(this)
        exportIds = savedInstanceState?.getLongArray("exportIds") ?: longArrayOf()
        val body = findViewById<LinearLayout>(R.id.telemetry_content)
        fun label(text: String) { body.addView(TextView(this).apply { this.text = text; setPadding(0, 12, 0, 6) }) }
        label("Session A (orange)")
        primary = Spinner(this).also { body.addView(it) }
        label("Compare with B (blue); aligned by elapsed time")
        secondary = Spinner(this).also { body.addView(it) }
        metric = Spinner(this).also { body.addView(it) }
        metric.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Power (W)", "Battery temperature (°C)", "FPS", "Frame interval peak /100 ms (ms)"))
        chart = TelemetryChart(this)
        body.addView(chart, LinearLayout.LayoutParams(-1, (250 * resources.displayMetrics.density).toInt()))
        summary = TextView(this).apply { setTextIsSelectable(true) }
        body.addView(summary)
        body.addView(Button(this).apply { text = "Refresh session list"; setOnClickListener { recreate() } })
        body.addView(Button(this).apply {
            text = "Export selected session(s) as CSV"
            setOnClickListener {
                exportIds = selectedIds()
                if (exportIds.isNotEmpty()) export.launch("scene-${System.currentTimeMillis()}.csv")
            }
        })
        label("Power is signed: charging +, discharging −. Wh and average W use only valid adjacent samples ≤10 s apart; gaps are excluded. Old records stay in the original history screens.\nFrame counts cover observed app-surface presentation intervals only. Long frame >3 target frame periods; very long >6. Counts may be incomplete when timestamps are unavailable or overwritten; they are not a universal Jank score. Set the target FPS before recording in Monitor settings.")
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { store.sessions(intent.getStringExtra("kind") ?: "game") } }
                .onSuccess { values ->
                    sessions = values
                    val date = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                    val names = values.map { "${date.format(Date(it.started))} · ${it.label} #${it.id}" }
                    primary.adapter = ArrayAdapter(this@ActivityTelemetry, android.R.layout.simple_spinner_dropdown_item, names)
                    secondary.adapter = ArrayAdapter(this@ActivityTelemetry, android.R.layout.simple_spinner_dropdown_item, listOf("None") + names)
                    primary.setSelection(values.indexOfFirst { it.id == savedInstanceState?.getLong("primaryId") }.coerceAtLeast(0))
                    secondary.setSelection(values.indexOfFirst { it.id == savedInstanceState?.getLong("secondaryId") } + 1)
                    metric.setSelection(savedInstanceState?.getInt("metric") ?: 0)
                    if (values.isEmpty()) summary.text = "No new recordings yet. Start charging or use the FPS recorder."
                    val listener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { load() }
                    }
                    primary.onItemSelectedListener = listener; secondary.onItemSelectedListener = listener; metric.onItemSelectedListener = listener
                    load()
                }.onFailure { summary.text = "Unable to read history: ${it.message}" }
        }
    }

    private fun selectedIds(): LongArray = listOfNotNull(sessions.getOrNull(primary.selectedItemPosition)?.id,
        sessions.getOrNull(secondary.selectedItemPosition - 1)?.id).distinct().toLongArray()

    private fun load() {
        if (!active || sessions.isEmpty()) return
        refresh?.cancel()
        refresh = scope.launch {
            while (isActive) {
                runCatching {
                    val ids = selectedIds()
                    val data = withContext(Dispatchers.IO) { ids.map { id ->
                        val session = sessions.first { it.id == id }
                        Report(session, store.samples(id), store.framePeaks(id), store.frameStats(id, session.targetFps))
                    } }
                    chart.unit = listOf("W", "°C", "FPS", "ms")[metric.selectedItemPosition.coerceAtLeast(0)]
                    chart.series = data.map { (_, samples, frames) ->
                        when(metric.selectedItemPosition) {
                            1 -> samples.map { it.elapsed to it.temperature }
                            2 -> samples.map { it.elapsed to it.fps }
                            3 -> frames.map { it.elapsed to it.duration }
                            else -> samples.map { it.elapsed to it.watts }
                        }
                    }
                    summary.text = data.mapIndexed { index, (session, samples, frames, stats) ->
                        val energy = TelemetryMath.energy(samples.map { it.elapsed }.toLongArray(), samples.map { it.watts }.toDoubleArray())
                        fun value(n: Double, unit: String) = if(n.isFinite()) "%.3f %s".format(n, unit) else "-- $unit"
                        val validFps = samples.map { it.fps }.filter { it.isFinite() }
                        val gap = samples.count { it.gap }
                        val elapsed = (samples.lastOrNull()?.elapsed ?: 0).coerceAtLeast(1)
                        val source = samples.lastOrNull()?.source.orEmpty()
                        "${if(index == 0) "A" else "B"} · ${session.label} #${session.id}\n" +
                            "Net energy ${value(energy[0], "Wh")} · average ${value(energy[1], "W")}\n" +
                            "Power coverage %.1f%% · %d samples\n".format(energy[2] / elapsed * 100, samples.size) +
                            "Average sampled FPS ${value(if(validFps.isEmpty()) Double.NaN else validFps.average(), "FPS")}\n" +
                            if(session.kind == "game") "Observed intervals ${stats.count} · target ${session.targetFps} FPS\n" +
                                (if(stats.count == 0L) "Long / very long frames: unavailable\n" else "Long ${stats.longCount} (includes very long ${stats.veryLongCount}) · max ${value(stats.maximum, "ms")}\n") +
                                "Timestamp gaps/unavailable samples $gap\n$source" else ""
                    }.joinToString("\n\n")
                }.onFailure { if(it !is CancellationException) summary.text = "Unable to load samples: ${it.message}" }
                delay(2000)
            }
        }
    }
    override fun onResume() { super.onResume(); active = true; if(::primary.isInitialized && sessions.isNotEmpty()) load() }
    override fun onPause() { active = false; refresh?.cancel(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray("exportIds", exportIds)
        outState.putLong("primaryId", sessions.getOrNull(primary.selectedItemPosition)?.id ?: -1)
        outState.putLong("secondaryId", sessions.getOrNull(secondary.selectedItemPosition - 1)?.id ?: -1)
        outState.putInt("metric", metric.selectedItemPosition.coerceAtLeast(0))
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        scope.coroutineContext[Job]?.invokeOnCompletion { store.close() }
        scope.cancel()
        super.onDestroy()
    }
}
