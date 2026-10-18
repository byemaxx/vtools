package com.omarea.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.omarea.store.SpfConfig

object LeakCanaryIcon {
    private fun component(context: Context) = ComponentName(context.packageName, "leakcanary.internal.activity.LeakLauncherActivity")

    fun isAvailable(context: Context): Boolean = try {
        context.packageManager.getActivityInfo(component(context), PackageManager.MATCH_DISABLED_COMPONENTS)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun apply(context: Context) {
        if (!isAvailable(context)) return
        val visible = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            .getBoolean(SpfConfig.GLOBAL_SPF_LEAK_ICON, false)
        val state = if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (context.packageManager.getComponentEnabledSetting(component(context)) != state) {
            context.packageManager.setComponentEnabledSetting(component(context), state, PackageManager.DONT_KILL_APP)
        }
    }
}
