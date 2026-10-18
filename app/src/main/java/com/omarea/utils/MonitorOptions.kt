package com.omarea.utils

import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.util.TypedValue
import com.omarea.vtools.R

object MonitorOptions {
    fun interval(preferences: SharedPreferences) = preferences.getInt("monitor_interval", 1500).coerceIn(500, 5000).toLong()
    fun apply(view: View, preferences: SharedPreferences, mini: Boolean) {
        fun visible(id: Int, key: String) {
            view.findViewById<View>(id)?.visibility = if (preferences.getBoolean(key, true)) View.VISIBLE else View.GONE
        }
        if (mini) {
            visible(R.id.fw_cpu_label, "monitor_cpu"); visible(R.id.fw_cpu_load, "monitor_cpu")
            visible(R.id.fw_gpu_label, "monitor_gpu"); visible(R.id.fw_gpu_load, "monitor_gpu")
            visible(R.id.fw_fps_label, "monitor_fps"); visible(R.id.fw_fps, "monitor_fps")
            visible(R.id.fw_battery_temp, "monitor_battery")
        } else {
            visible(R.id.fw_cpu, "monitor_cpu"); visible(R.id.fw_gpu, "monitor_gpu"); visible(R.id.fw_battery, "monitor_battery")
        }
        val scale = preferences.getFloat("monitor_scale", 1f).coerceIn(0.75f, 2f)
        fun resize(item: View) {
            item.layoutParams?.let {
                if (it.width > 0) it.width = (it.width * scale).toInt()
                if (it.height > 0) it.height = (it.height * scale).toInt()
            }
            item.setPadding((item.paddingLeft * scale).toInt(), (item.paddingTop * scale).toInt(),
                (item.paddingRight * scale).toInt(), (item.paddingBottom * scale).toInt())
            item.minimumWidth = (item.minimumWidth * scale).toInt()
            item.minimumHeight = (item.minimumHeight * scale).toInt()
            if (item is TextView) item.setTextSize(TypedValue.COMPLEX_UNIT_PX, item.textSize * scale)
            if (item is ViewGroup) for (index in 0 until item.childCount) resize(item.getChildAt(index))
        }
        resize(view)
    }
}
