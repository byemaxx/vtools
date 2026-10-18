package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic
import com.omarea.utils.TelemetryMath

class FrameTimeline {
    data class Batch(val durations: List<Double>, val source: String, val gap: Boolean, val endTimes: List<Long> = emptyList())
    private val shell = KeepShellPublic.getInstance("frame-timeline", true)
    private var layer: String? = null
    private var previous = 0L
    private var candidateIndex = 0

    fun read(packageName: String): Batch {
        if (!packageName.matches(Regex("[a-zA-Z0-9._]+"))) return Batch(emptyList(), "Unavailable", true)
        if (layer == null) {
            val candidates = shell.doCmdSync("dumpsys SurfaceFlinger --list").lineSequence()
                .filter { it.contains(packageName) }.sortedByDescending { it.contains("SurfaceView") }.toList()
            layer = if (candidates.isEmpty()) null else candidates[candidateIndex % candidates.size]
            previous = 0L
        }
        val selected = layer ?: return Batch(emptyList(), "No app surface", true)
        val quoted = "'" + selected.replace("'", "'\\''") + "'"
        val output = shell.doCmdSync("dumpsys SurfaceFlinger --latency $quoted")
        val result = TelemetryMath.frames(output, previous)
        if (result.last == 0L) {
            candidateIndex++
            layer = null
            return Batch(emptyList(), "Frame timestamps unavailable", true)
        }
        if (result.last == previous) {
            candidateIndex++
            layer = null
            return Batch(emptyList(), "No new frame timestamps", true)
        }
        previous = result.last
        return Batch(result.durations, "SurfaceFlinger: $selected", result.gap, result.endTimes)
    }
}
