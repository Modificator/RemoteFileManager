package com.patchself.goserver

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


object FileServer {
    lateinit var webResPath: File

    init {
        System.loadLibrary("FileServer")
    }

    fun init(context: Context) {
        webResPath = File(context.filesDir, "webRes")
        webResPath.delete()
        context.assets.copyAssetFolder("", webResPath.absolutePath)
    }

    fun startServer(listen: String) {
        startServer(webResPath.absolutePath, listen)
    }

    external fun startServer(webResPath: String, listen: String)
    external fun stopServer()

    fun AssetManager.copyAssetFolder(srcName: String, dstName: String): Boolean {
        var srcName = srcName.trimStart('/')
        return try {
            var result = true
            val fileList = this.list(srcName) ?: return false
            if (fileList.isEmpty()) {
                result = copyAssetFile(srcName, dstName)
            } else {
                val file = File(dstName)
                result = file.mkdirs()
                for (filename in fileList) {
                    result = result and copyAssetFolder(
                        srcName + separator.toString() + filename,
                        dstName + separator.toString() + filename
                    )
                }
            }
            result
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun AssetManager.copyAssetFile(srcName: String, dstName: String): Boolean {
        return try {
            val inStream = this.open(srcName)
            val outFile = File(dstName)
            val out: OutputStream = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            inStream.close()
            out.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}