package com.flutter.cabinet_plugin

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.flutter.cabinet_plugin.entities.ControlData
import com.flutter.cabinet_plugin.entities.Result
import com.flutter.cabinet_plugin.enums.DeviceType
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.usb.SocketCallBack
import com.flutter.cabinet_plugin.util.CommandUtils
import com.flutter.cabinet_plugin.util.Constants
import com.flutter.cabinet_plugin.util.HexData
import com.flutter.cabinet_plugin.util.RFIDUtils
import com.google.gson.Gson
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.net.ServerSocket
import java.net.Socket

class SocketClientDelegate(private val context: Context, private val callBack: SocketCallBack) : Thread() {
    //flutter数据交互通道
    var result: MethodChannel.Result? = null

    //handler
    private val handler = Handler()

    //gson
    private val g = Gson()

    private var transferSocket: TransferSocket? = null

    //循环连接标志
    var connect: Boolean = false
    override fun run() {
        super.run()
        startService(context)
    }

    //连接
    private fun startService(context: Context) {
        //use实现了Closeable接口,其参数在函数结束后会自动关闭,调用其close方法,无论是否发生异常.
        while (true) {
            try {
                //判定是否需要连接
                if (!connect) {
                    Socket("192.168.0.30", 10086).use { socket ->
                        val address = socket.inetAddress.hostAddress
                        val channel = getId(address).toInt()
                        transferSocket = TransferSocket(socket, channel, ::reportData)
                        connect = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //重新连接
                connect = false
            }
            // 每次休眠2s
            sleep(2000)
        }
    }


    fun transferData(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        this.result = result
        when (call.method) {
            "db_file_upload" -> uploadDbFile()
            "resource_file_upload" -> uploadResourceFile()
            "db_file_download" -> downloadDbFile()
            "resource_file_download" -> downloadResourceFile()
            "authenticate" -> {
                val password = call.argument<String>("password") ?: ""
                authenticate(password)
            }
            "update_authenticate" -> {
                val password = call.argument<String>("password") ?: ""
                updateAuthenticate(password)
            }
            else -> {
            }
        }
    }

    private fun uploadDbFile() {
        println("socket=$transferSocket")
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            transferSocket?.addCommand(CommandUtils.uploadDbFileCommand(channel))
        }
    }

    private fun uploadResourceFile() {
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            transferSocket?.addCommand(CommandUtils.uploadResourceCommand(channel))
        } else {
            postResult(Result(false, "无法与目标通信"))
        }
    }

    private fun downloadDbFile() {
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            transferSocket?.addCommand(CommandUtils.downloadDbFile(channel))
        } else {
            postResult(Result(false, "无法与目标通信"))
        }
    }

    private fun downloadResourceFile() {
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            transferSocket?.addCommand(CommandUtils.downloadResourceFile(channel))
        } else {
            postResult(Result(false, "无法与目标通信"))
        }
    }

    private fun authenticate(password: String) {
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            val dataArray = password.toByteArray()
            transferSocket?.addCommand(CommandUtils.authenticate(channel, dataArray))
        } else {
            postResult(Result(false, "无法与目标通信"))
        }
    }

    private fun updateAuthenticate(password: String) {
        val device = transferSocket?.device
        if (device != null && transferSocket != null) {
            val channel = device.channel.toByte()
            val dataArray = password.toByteArray()
            transferSocket?.addCommand(CommandUtils.updateAuthenticate(channel, dataArray))
        } else {
            postResult(Result(false, "无法与目标通信"))
        }
    }

    //获取当前通道情况
    fun channel(): Int? {
        //返回结果
        return transferSocket?.device?.channel
    }

    //获取通道ip
    private fun getId(ipAddress: String): String {
        return ipAddress.split(Regex("\\.")).last()
    }


    //上报数据给界面
    private fun reportData(channel: Int, controlData: ControlData) {
        when (controlData.control) {
            Order.RESPONSE_DB_FILE_UPLOAD.type -> {//上传数据库变动a
            }
            Order.RESPONSE_RESOURCE_FILE_UPLOAD.type -> {//上传资源文件
            }
            Order.RESPONSE_DB_FILE_DOWNLOAD.type -> {
            }
            Order.RESPONSE_RESOURCE_FILE_DOWNLOAD.type -> {
            }
            Order.RESPONSE_AUTHENTICATE.type -> {
            }
            Order.RESPONSE_UPDATE_AUTHENTICATE.type -> {
            }
            Order.RESPONSE_CHANNEL_CHANGE.type -> {
                val data = controlData.data as Boolean
                //判断通道的状态
                if (!data) transferSocket = null
                //通道建立
                handler.post { callBack.socketChange() }
            }
        }

    }


    //回传结果
    private fun postResult(data: Any?) {
        //result只能传递简单的对象，当复杂对象时会出现错误
        val resultData = data?.let { g.toJson(it) }
        handler.post {
            try {
                result?.success(resultData)
                result = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}