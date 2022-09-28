package com.patchself.filemanager

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.patchself.goserver.FileServer
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    val REQUEST_PERMISSION = 0xf01
    var btnSwitch: MaterialSwitch? = null
    var tvStatus: TextView? = null
    var tvStatusDetails: TextView? = null
    var listenPort = 23333

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusDetails = findViewById(R.id.tvStatusDetails)
        btnSwitch = findViewById<MaterialSwitch>(R.id.btnSwitch)
        btnSwitch!!.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                tvStatus?.text = "已开启"
                tvStatusDetails?.isGone = false
                tvStatusDetails?.text = "请使用浏览器访问"
                networkChangeReceiver.onReceive(this, intent)
                checkStartFileServer()
            } else {
                tvStatus?.text = "已关闭"
                tvStatusDetails?.isGone = true
                FileServer.stopServer()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(networkChangeReceiver, filter)
        } catch (e: Throwable) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(networkChangeReceiver)
        } catch (e: Throwable) {
        }
    }

    private fun checkStartFileServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                btnSwitch?.post {
                    btnSwitch?.isChecked = false
                }
                MaterialAlertDialogBuilder(this)
                    .setMessage("需要管理所有文件的权限才能正常使用此功能")
                    .setPositiveButton("去开启") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.fromParts("package", packageName, null)
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("取消") { _, _ ->
                        btnSwitch?.isChecked = false
                    }
                    .show()
            } else {
                startFileServer()
            }
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                //提示为啥需要权限
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_PERMISSION
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_PERMISSION
                )
            }
        } else {
            startFileServer()
        }
    }

    private fun startFileServer() {
        thread {
            FileServer.init(this)
            FileServer.startServer(":$listenPort")
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (permissions.size == 2 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startFileServer()
            } else {
                btnSwitch?.isChecked = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    //提示没有权限不能干啥
                } else {
                    //提示进入设置开启权限, 跳转到设置
                    MaterialAlertDialogBuilder(this)
                        .setTitle("权限错误")
                        .setMessage("需要获得存储权限才能正常使用此功能")
                        .setPositiveButton("去设置开启") { _, _ ->
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            intent.data = Uri.fromParts("package", packageName, null)
                            startActivity(intent)
                        }
                        .setNegativeButton("取消") { _, _ -> }
                        .show()
                }
            }
        }
    }


    var networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            tvStatusDetails?.text = if (getAllConnectIP().isNotBlank()) {
                "请使用浏览器访问：\n" + getAllConnectIP().lineSequence().map { "http://$it:$listenPort" }
                    .joinToString("\n")
            } else {
                "请连接 Wifi 后重试"
            }
        }
    }

    fun getAllConnectIP(): String {
        var result = ""
        try {
            var networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces() ?: return ""
            while (networkInterfaceEnumeration.hasMoreElements()) {
                var source = networkInterfaceEnumeration.nextElement()
                for (interfaceAddress in source.interfaceAddresses) {
                    var addr = interfaceAddress.address
                    if (addr is Inet4Address && !addr.isAnyLocalAddress && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        var host = addr.hostAddress ?: ""
                        if (host.isEmpty() || host == "172.21.0.1") {
                            continue
                        }
                        result += host + "\n"
                    }
                }
            }
        } catch (e: Exception) {

        }
        return result.trim()
    }
}