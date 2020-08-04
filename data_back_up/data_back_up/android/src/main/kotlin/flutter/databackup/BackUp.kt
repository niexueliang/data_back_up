package flutter.databackup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.fs.UsbFileOutputStream
import io.flutter.plugin.common.PluginRegistry
import java.io.File


class BackUp(private val activity: Activity) : UsbReceiver.UsbCallBack, PluginRegistry.RequestPermissionsResultListener {
    var type = ""
    var name: String = ""
    var content: String = ""
    var usbMassStorageDevice: UsbMassStorageDevice? = null


    fun back(backType: String, backName: String, content: String) {
        this.type = backType
        this.name = backName
        this.content = content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission()
        } else {
            backData()
        }


    }

    private fun requestPermission() {
        //拒绝了部分权限
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_PERMISSION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, WRITE_PERMISSION)) {
            //跳转到Settings页面
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
        } else {
            // 申请授权。
            if (ContextCompat.checkSelfPermission(activity, READ_PERMISSION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(activity, arrayOf(READ_PERMISSION, WRITE_PERMISSION), READ_WRITE_CODE);
            } else if (ContextCompat.checkSelfPermission(activity, READ_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(READ_PERMISSION), READ_CODE);
            } else if (ContextCompat.checkSelfPermission(activity, WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(WRITE_PERMISSION), WRITE_CODE);
            } else {
                backData()
            }
        }
    }

    fun backData() {
        when (type) {
            "sd" -> {
                backSd(name, content)
            }
            "usb" -> {
                backUsb(name, content)
            }
            else -> {
                Log.e("back_up", "未知数据备份方式")
            }
        }
    }

    //根据传入类型获取otg
    fun backSd(name: String, content: String) {
        this.name = name
        this.content = content
        writeToStorage()
    }

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

    fun backUsb(name: String, content: String) {
        this.name = name
        this.content = content
        //设备集合
        val deviceList = getDeviceList(activity)
        if (deviceList.isNotEmpty()) {
            deviceList[0].apply {
                usbMassStorageDevice = this
                UsbReceiver.requestPermission(activity, this)
            }
        }
    }


    //获取挂在的所有的路径
    fun getDeviceList(context: Context): Array<UsbMassStorageDevice> {
        return UsbMassStorageDevice.getMassStorageDevices(context);
    }

    //获取挂在路径
    fun getDevicePathList(context: Context): List<String> {
        val devices = UsbMassStorageDevice.getMassStorageDevices(context)
        return devices.map {
            it.init();
            it.partitions[0].fileSystem.rootDirectory.absolutePath
        };
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
            val localFile = root.search(name)
            localFile?.delete()
            //创建文件
            val usbFile = root.createFile(name)
            //获取写入流
            val os = UsbFileOutputStream(usbFile)
            os.write(content.toByteArray())
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        device.close()
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
            Log.e(TAG, "文件不存在")
        }
        device.close()
        return null
    }

    override fun usbDeviceChecked(flag: Boolean) {
        Log.e(TAG, "sub是否授权$flag")
        usbMassStorageDevice?.let { writeFile(it) }
    }

    override fun usbAttached() {
        Log.e(TAG, "usbAttached")
    }

    override fun usbDetached() {
        Log.e(TAG, "usbDetached")
    }

    companion object {
        const val TAG = "BackUp"
        const val READ_WRITE_CODE = 100;
        const val READ_CODE = 98;
        const val WRITE_CODE = 99;
        const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val REQUEST_CODE_WRITE_SETTINGS = 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        if (permissions?.size != grantResults?.size) return false
        val refusePermission = arrayListOf<String>()
        grantResults?.forEachIndexed { index, result ->
            if (result != PackageManager.PERMISSION_GRANTED) {
                refusePermission.add(permissions!![index])
            }
        }
        if (refusePermission.size > 0) {//拒绝了某些权限

        } else {
            backData()
        }
        return false
    }


}