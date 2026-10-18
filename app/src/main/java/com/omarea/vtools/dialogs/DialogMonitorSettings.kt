package com.omarea.vtools.dialogs

import android.content.Context
import android.widget.*
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.popup.FloatMonitor
import com.omarea.vtools.popup.FloatMonitorMini

class DialogMonitorSettings {
    fun show(context: Context) {
        val preferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 12) }
        val checks = linkedMapOf<String, CheckBox>()
        for ((key, text) in listOf("monitor_cpu" to "CPU", "monitor_gpu" to "GPU", "monitor_fps" to "FPS",
            "monitor_battery" to "Battery temperature + power", "monitor_details" to "Expanded details on open")) {
            checks[key] = CheckBox(context).apply {
                this.text = text; isChecked = preferences.getBoolean(key, key != "monitor_details"); body.addView(this)
            }
        }
        fun label(text: String) { body.addView(TextView(context).apply { this.text = text }) }
        label("Monitor size")
        val scales = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val scale = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, scales.map { "${(it * 100).toInt()}%" })
            setSelection(scales.indexOf(preferences.getFloat("monitor_scale", 1f)).coerceAtLeast(0)); body.addView(this)
        }
        label("Monitor refresh interval (FPS recording remains 1 s)")
        val intervals = listOf(500, 1000, 1500, 2000, 3000, 5000)
        val interval = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, intervals.map { "$it ms" })
            setSelection(intervals.indexOf(preferences.getInt("monitor_interval", 1500)).coerceAtLeast(0)); body.addView(this)
        }
        label("Game target FPS (long frame >3 target periods; very long >6)")
        val targets = listOf(30, 60, 90, 120, 144, 165, 240)
        val target = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, targets.map { "$it FPS" })
            setSelection(targets.indexOf(preferences.getInt("monitor_target_fps", 60)).coerceAtLeast(0)); body.addView(this)
        }
        label("Faster refresh adds measurement overhead. Frame thresholds apply to new recordings.")
        val save = Button(context).apply { text = "Save"; body.addView(this) }
        val cancel = Button(context).apply { text = "Cancel"; body.addView(this) }
        val dialog = DialogHelper.customDialog(context, ScrollView(context).apply { addView(body) })
        cancel.setOnClickListener { dialog.dismiss() }
        save.setOnClickListener {
            if (listOf("monitor_cpu", "monitor_gpu", "monitor_battery").none { checks.getValue(it).isChecked }) {
                Toast.makeText(context, "Keep CPU, GPU or battery visible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            preferences.edit().apply {
                checks.forEach { (key, check) -> putBoolean(key, check.isChecked) }
                putFloat("monitor_scale", scales[scale.selectedItemPosition])
                putInt("monitor_interval", intervals[interval.selectedItemPosition])
                putInt("monitor_target_fps", targets[target.selectedItemPosition]); apply()
            }
            if (FloatMonitor.show == true) { FloatMonitor(context).hidePopupWindow(); FloatMonitor(context).showPopupWindow() }
            if (FloatMonitorMini.show == true) { FloatMonitorMini(context).hidePopupWindow(); FloatMonitorMini(context).showPopupWindow() }
            dialog.dismiss()
        }
    }
}
