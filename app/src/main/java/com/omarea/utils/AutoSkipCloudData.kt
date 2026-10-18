@file:OptIn(DelicateCoroutinesApi::class)

package com.omarea.utils

import android.content.Context
import com.omarea.Scene
import com.omarea.store.AutoSkipConfigStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

class AutoSkipCloudData {
    fun updateConfig(context: Context, showMsg: Boolean) {
        val appContext = context.applicationContext
        Scene.backgroundScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://vtools.oss-cn-beijing.aliyuncs.com/addin/auto-skip-config-v1.json")
                val connection = url.openConnection()
                // 设置连接方式：get
                // connection.setRequestMethod("GET");
                // 设置连接主机服务器的超时时间：15000毫秒
                connection.connectTimeout = 15000
                // 设置读取远程返回的数据时间：60000毫秒
                connection.readTimeout = 20000
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val data = JSONArray(response)
                AutoSkipConfigStore(appContext).use { db ->
                    val database = db.writableDatabase
                    database.beginTransaction()
                    try {
                        check(db.clearAll())
                        for (index in 0 until data.length()) {
                            val row = data.getJSONObject(index)
                            check(db.addConfig(row.getString("activity"), row.getString("viewId")))
                        }
                        database.setTransactionSuccessful()
                    } finally {
                        database.endTransaction()
                    }
                }
                if (showMsg) {
                    Scene.toast("Fetched " + data.length() + " auto-skip entries from the cloud")
                }
            } catch (ex: Exception) {
                if (showMsg) {
                    Scene.toast("Failed to fetch cloud config data")
                }
            }
        }
    }
}
