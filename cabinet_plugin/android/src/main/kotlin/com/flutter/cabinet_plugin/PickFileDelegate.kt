package com.flutter.cabinet_plugin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flutter.cabinet_plugin.util.Constants.FILE_SELECTOR_CODE
import com.flutter.cabinet_plugin.util.Constants.READ_CODE
import com.flutter.cabinet_plugin.util.Constants.READ_PERMISSION
import com.flutter.cabinet_plugin.util.Constants.READ_WRITE_CODE
import com.flutter.cabinet_plugin.util.Constants.REQUEST_CODE_WRITE_SETTINGS
import com.flutter.cabinet_plugin.util.UriToFile
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


class PickFileDelegate(private val activity: Activity) : PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {
    private var result: MethodChannel.Result? = null
    private var mimeTypes: Array<String>? = null
    fun pickFile(result: MethodChannel.Result, mimeTypes: Array<String>?) {
        this.result = result
        this.mimeTypes = mimeTypes
        requestPermission()
    }

    private fun pick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        if (mimeTypes != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.type = if (mimeTypes!!.size == 1) mimeTypes!![0] else "*/*"
                if (mimeTypes!!.isNotEmpty()) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                }
            } else {
                var mimeTypesContent = ""
                for (mimeType in mimeTypes!!) {
                    mimeTypesContent += "$mimeType|"
                }
                intent.type = mimeTypesContent.substring(0, mimeTypes!!.size - 1)
            }
        }
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        activity.startActivityForResult(intent, FILE_SELECTOR_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == FILE_SELECTOR_CODE && resultCode == Activity.RESULT_OK) {
            println("uri:$data")
            val uri: Uri = data?.data ?: return true
            val path = UriToFile.getPath(activity, uri) ?: ""
            result?.success(path)
            //马上置空 防止重复提交
            result = null
        }
        return true
    }


    //请求usb读写权限
    private fun requestPermission() {
        //拒绝了部分权限
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_PERMISSION)) {
            println("requestPermission 拒绝权限，跳转到settings页面")
            //跳转到Settings页面
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
        } else {
            println("申请权限")
            when {
                ContextCompat.checkSelfPermission(activity, READ_PERMISSION) != PackageManager.PERMISSION_GRANTED -> {
                    println("requestPermission 请求读写权限")
                    ActivityCompat.requestPermissions(activity, arrayOf(READ_PERMISSION), READ_WRITE_CODE)
                }
                ContextCompat.checkSelfPermission(activity, READ_PERMISSION) != PackageManager.PERMISSION_GRANTED -> {
                    println("requestPermission 请求读权限")
                    ActivityCompat.requestPermissions(activity, arrayOf(READ_PERMISSION), READ_CODE)
                }
                else -> {
                    println("已经授权，直接调用")
                    pick()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>?,
            grantResults: IntArray?
    ): Boolean {
        if (permissions?.size != grantResults?.size) return false
        val refusePermission = arrayListOf<String>()
        grantResults?.forEachIndexed { index, result ->
            if (result != PackageManager.PERMISSION_GRANTED) {
                refusePermission.add(permissions!![index])
            }
        }
        if (refusePermission.size > 0) {//拒绝了某些权限
            println("权限没有完全授予")
        } else {
            println("授权完成：onRequestPermissionsResult")
            pick()
        }
        return true
    }
}