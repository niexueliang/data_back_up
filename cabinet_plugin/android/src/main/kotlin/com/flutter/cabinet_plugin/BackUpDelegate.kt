package com.flutter.cabinet_plugin

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flutter.cabinet_plugin.util.Constants
import com.flutter.cabinet_plugin.util.Constants.ACTION_USB_PERMISSION
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.fs.UsbFileOutputStream
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

class BackUpDelegate(private val activity: Activity) :
        PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener, CoroutineScope {
    //默认备份到本地磁盘
    private val job = SupervisorJob()
    private var type = 0
    private var name: String = ""
    private var content: String = ""
    private var usbMassStorageDevice: UsbMassStorageDevice? = null
    private var result: MethodChannel.Result? = null
    private val handler = Handler()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO


    //写文件
    fun writeFile(result: MethodChannel.Result, backType: Int, backName: String, content: String) = launch {
        this@BackUpDelegate.result = result
        this@BackUpDelegate.type = backType
        this@BackUpDelegate.name = backName
        this@BackUpDelegate.content = content
        if (name.isEmpty() || content.isEmpty()) return@launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission()
        } else {
            println("不需要授权,直接备份")
            backData()
        }
    }

    //判定usb是否挂载
    fun checkUsbMount(): Boolean {
        return getDeviceList(activity).isNotEmpty()
    }

    //读文件
    fun readFile(device: UsbMassStorageDevice, name: String): ByteArray? {
        //初始化
        device.init()
        //获取当前挂载系统
        val currentFs = device.partitions[0].fileSystem
        val root = currentFs.rootDirectory
        val usbFile = root.search(name)
        if (usbFile != null) {
            val inputStream = UsbFileInputStream(usbFile)
            val buffer = ByteArray(currentFs.chunkSize)
            inputStream.read(buffer)
            return buffer
        } else {
            println("文件不存在")
        }
        device.close()
        return null
    }

    //备份示数据
    private fun backData() {
        if (name.isEmpty() || content.isEmpty()) return
        println("backType=$type:::backName=$name:::content=$content")
        try {
            when (type) {
                0 -> {
                    writeToStorage()
                }
                1 -> {
                    backUsb()
                }
                else -> {
                    println("未知数据备份方式")
                }
            }
            handler.post { result?.success(true) }

        } catch (e: Exception) {
            e.printStackTrace()
            result?.success(false)
            handler.post { result?.success(true) }
        }
    }

    //写数据到本地
    private fun writeToStorage() {
        var rootPath = Environment.getExternalStorageDirectory()?.absolutePath
        if (rootPath.isNullOrEmpty()) {
            rootPath = activity.cacheDir?.absolutePath
        }
        if (rootPath == null) {
            throw Exception("不存在内置存储,无法备份文件")
        }
        val backPath = rootPath + File.separator + name
        val backFile = File(backPath)
        backFile.writeBytes(content.toByteArray())
    }

    //usb方式备份
    private fun backUsb() {
        //设备集合
        val deviceList = getDeviceList(activity)
        if (deviceList.isNotEmpty()) {
            deviceList[0].apply {
                usbMassStorageDevice = this
                requestUsbPermission(this.usbDevice)
            }
        } else {
            println("USB设备不存在")
        }
    }

    //获取挂在的所有的路径
    private fun getDeviceList(context: Context): Array<UsbMassStorageDevice> {
        return UsbMassStorageDevice.getMassStorageDevices(context)
    }

    //写文件
    private fun writeFile(device: UsbMassStorageDevice) {
        //初始化
        device.init()
        //获取当前挂载系统
        try {
            val currentFs = device.partitions[0].fileSystem
            //获取根路径
            val root = currentFs.rootDirectory
            //创建旧文件
            var localFile = root.search(name)
            if (localFile == null) {
                //创建文件
                localFile = root.createFile(name)
            }
            //获取写入流
            val os = UsbFileOutputStream(localFile, true)
            os.write(content.toByteArray())
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            device.close()
        }
    }

    //向usb写数据
    fun writeUsbWithPermission() {
        usbMassStorageDevice?.let(::writeFile)
    }

    //请求usb读写权限
    private fun requestPermission() {
        //拒绝了部分权限
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Constants.READ_PERMISSION
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Constants.WRITE_PERMISSION
                )
        ) {
            println("requestPermission 拒绝权限，跳转到settings页面")
            //跳转到Settings页面
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent, Constants.REQUEST_CODE_WRITE_SETTINGS)
        } else {
            println("申请权限")
            if (ContextCompat.checkSelfPermission(
                            activity,
                            Constants.READ_PERMISSION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                            activity,
                            Constants.WRITE_PERMISSION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("requestPermission 请求读写权限")
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Constants.READ_PERMISSION, Constants.WRITE_PERMISSION),
                        Constants.READ_WRITE_CODE
                )
            } else if (ContextCompat.checkSelfPermission(
                            activity,
                            Constants.READ_PERMISSION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("requestPermission 请求读权限")
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Constants.READ_PERMISSION),
                        Constants.READ_CODE
                )
            } else if (ContextCompat.checkSelfPermission(
                            activity,
                            Constants.WRITE_PERMISSION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("requestPermission 请求写权限")
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Constants.WRITE_PERMISSION),
                        Constants.WRITE_CODE
                )
            } else {
                println("已经授权，直接调用")
                backData()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        if (permissions?.size != grantResults?.size) return false
        val refusePermission = arrayListOf<String>()
        grantResults?.forEachIndexed { index, result ->
            if (result != PackageManager.PERMISSION_GRANTED) refusePermission.add(permissions!![index])
        }
        if (refusePermission.size > 0) {//拒绝了某些权限
            println("权限没有完全授予")
        } else {
            println("授权完成：onRequestPermissionsResult")
            backData()
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == Constants.REQUEST_CODE_WRITE_SETTINGS) {
            println("onActivityResult=页面切换")
            requestPermission()
        }
        return false
    }

    private fun requestUsbPermission(usbDevice: UsbDevice) {
        //不能直接读写，请求u盘操作权限
        val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        if (usbManager.hasPermission(usbDevice)) {
            writeUsbWithPermission()
        } else {
            println("权限未授予，请求权限")
            val intent = PendingIntent.getBroadcast(activity, 0, Intent(ACTION_USB_PERMISSION), 0)
            usbManager.requestPermission(usbDevice, intent)
        }
    }

}