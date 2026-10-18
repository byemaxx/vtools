@file:OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)

package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.CheckBox
import com.omarea.common.shell.KeepShell
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.FileOwner
import com.omarea.library.basic.UninstalledApp
import com.omarea.model.AppInfo
import com.omarea.ui.AdapterAppList
import com.omarea.vtools.R
import com.omarea.vtools.databinding.ActivityAppRetrieveBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class ActivityAppRetrieve : ActivityBase() {
    private lateinit var progressBarDialog: ProgressBarDialog
    private var adapterAppList: WeakReference<AdapterAppList>? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pm: PackageManager
    private val keepShell = KeepShell()
    private val pageScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private lateinit var binding: ActivityAppRetrieveBinding

    override fun onDestroy() {
        pageScope.cancel()
        handler.removeCallbacksAndMessages(null)
        progressBarDialog.hideDialog()
        keepShell.tryExit()
        super.onDestroy()
    }

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppRetrieveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setBackArrow()

        pm = packageManager
        progressBarDialog = ProgressBarDialog(this)
        binding.hiddenApp.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))
        binding.fabConfirm.setOnClickListener {
            this.onConfirm()
        }
    }

    private fun getAppInfo(it: ApplicationInfo): AppInfo {
        val app = AppInfo()
        app.appName = "" + it.loadLabel(pm)
        app.packageName = it.packageName
        app.enabled = it.enabled
        app.path = it.sourceDir
        return app
    }

    private fun loadData() {
        progressBarDialog.showDialog("Fetching app status")

        loadJob?.cancel()
        loadJob = pageScope.launch {
            val appList = try {
                withContext(Dispatchers.IO) {
                    ArrayList(UninstalledApp().getUninstalledApp(applicationContext).map { getAppInfo(it) })
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("Scene", "Unable to load applications", error)
                progressBarDialog.hideDialog()
                android.widget.Toast.makeText(this@ActivityAppRetrieve, R.string.app_list_load_failed, android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            progressBarDialog.hideDialog()
            val adapterObj = AdapterAppList(context, appList)
            binding.hiddenApp.adapter = adapterObj
            adapterAppList = WeakReference(adapterObj)
            binding.hiddenApp.onItemClickListener = AdapterView.OnItemClickListener { _, itemView, postion, _ ->
                if (postion == 0) {
                    val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                    checkBox.isChecked = !checkBox.isChecked
                    if (adapterAppList?.get() != null) {
                        adapterAppList?.get()!!.setSelecteStateAll(checkBox.isChecked)
                        adapterAppList?.get()!!.notifyDataSetChanged()
                    }
                } else {
                    val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                    checkBox.isChecked = !checkBox.isChecked
                    val all = binding.hiddenApp.findViewById<CheckBox>(R.id.select_state_all)
                    if (adapterAppList?.get() != null) {
                        all.isChecked = adapterAppList?.get()!!.getIsAllSelected()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun onConfirm() {
        // 获取选中项
        val items = adapterAppList?.get()?.getSelectedItems() ?: return
        if (items.size > 0) {
            val cmds = StringBuilder()
            for (app in items) {
                cmds.append("pm unhide ${app.packageName}\n")
                cmds.append("pm enable ${app.packageName}\n")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cmds.append("pm unsuspend ${app.packageName}\n")
                }
            }

            progressBarDialog.showDialog(getString(R.string.please_wait))
            Thread {
                keepShell.doCmdSync(cmds.toString())
                reInstallAppShell(items)

                val uninstalledApp = UninstalledApp().getUninstalledApp(this)
                val fail: ArrayList<AppInfo> = ArrayList()
                for (app in uninstalledApp) {
                    val result = items.filter { it.packageName == app.packageName }
                    if (result.isNotEmpty()) {
                        fail.add(getAppInfo(app))
                    }
                }

                handler.post {
                    if (isFinishing || isDestroyed) return@post
                    progressBarDialog.hideDialog()
                    if (fail.size > 0) {
                        val msg = StringBuilder()
                        for (app in fail) {
                            msg.append(app.appName)
                            msg.append("\n")
                        }

                        DialogHelper.helpInfo(this, "The following apps could not be restored", msg.toString() + "\n\nYou can try backing up and deleting /data/system/users/0/package-restrictions.xml in Recovery (TWRP) mode.")

                        if (uninstalledApp.size != items.size) {
                            loadData()
                        }
                    } else {
                        loadData()
                    }
                }
            }.start()
        }
    }

    // 如果恢复不了，也可修改 /data/system/users/$uid/package-restrictions.xml
    private fun reInstallAppShell(apps: ArrayList<AppInfo>) {
        val uid = FileOwner(this).userId
        for (app in apps) {
            val cmd = "pm install-existing --user $uid ${app.packageName}"
            Log.d("Scene", cmd)
            keepShell.doCmdSync(cmd)
        }
    }
}
