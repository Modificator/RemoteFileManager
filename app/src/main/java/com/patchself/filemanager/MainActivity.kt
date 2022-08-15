package com.patchself.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import androidx.core.app.ActivityCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.patchself.goserver.FileServer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    val REQUEST_PERMISSION = 0xf01
    var btnSwitch: MaterialSwitch?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnSwitch = findViewById<MaterialSwitch>(R.id.btnSwitch)
        btnSwitch!!.setOnCheckedChangeListener { compoundButton, b ->
            if (b){
                startFileServer()
            }else{
                FileServer.stopServer()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    fun startFileServer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //提示为啥需要权限
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION)
            }
        } else {
            thread {
                FileServer.init(this)
                FileServer.startServer(":12312")
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (permissions.size==2){
                startFileServer()
            }else{
                btnSwitch?.isChecked = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    //提示没有权限不能干啥
                } else {
                    //提示进入设置开启权限, 跳转到设置
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                }
            }
        }
    }
}