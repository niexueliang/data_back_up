package com.flutter.fluttercommunicate

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SocketService {
    //flutter数据交互通道
    var result: MethodChannel.Result? = null

    //socket服务器
    private var server: ServerSocket? = null

    //线程池
    private var mExecutorService: ExecutorService? = null

    //通道列表
    private var transMap = hashMapOf<String, TransferSocket>()

    //数据库管理
    private var deviceDao: DeviceDao? = null

    private val handler = Handler()

    private val gson = Gson()

    //启动监听
    fun startService(context: Context) {
        println("startService")
        try {
            deviceDao = DeviceDao.getInstance(context)
            server = ServerSocket(PORT)
            mExecutorService = Executors.newCachedThreadPool()
            mExecutorService!!.execute {
                while (true) {
                    val client = server!!.accept()
                    val address = client.inetAddress.hostAddress
                    val channel = getId(address)
                    println("建立socket连接::channel=$channel")
                    val transferSocket = TransferSocket(client, channel, ::reportData)
                    transMap[channel] = transferSocket
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun transferData(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        this.result = result
        when (call.method) {
            "getDevices" -> {
                getDevices()
            }
            "identification" -> {
                identification()
            }
            "scan" -> {
                val channel = call.argument<String>("channel")
                val cell = call.argument<String>("cell")
                if (channel != null && cell != null) {
                    scanning(channel, cell)
                }
            }
            else -> {
            }
        }
    }


    //识别rfid
    private fun identification() {
        Log.e("nil", "身份认证")
        val devices = deviceDao?.getDevices()
        val device = devices?.find { it.type == 0 }
        if (device != null) {
            val transferSocket = transMap["${device.id}"]
            if (transferSocket != null) {
                val command = CommandUtils.getCardAuthentication(device.id.toByte(), 0.toByte())
                transferSocket.addCommand(command)
            }
        }
    }

    //扫描指定柜子 指定格子的rfid
    private fun scanning(cabinet: String, cell: String) {
        Log.e("nil", "扫描===>柜子编号=$cabinet:::格子编号=$cell")
        val transferSocket = transMap[cabinet]
        transferSocket?.let {
            val command = CommandUtils.getCardAuthentication(cabinet.toInt().toByte(), cell.toInt().toByte())
            it.addCommand(command)
        }
    }

    //获取设备信息
    private fun getDevices() {
        val devices = deviceDao?.getDevices()
        Log.e("nil", "devices=$devices")
        postResult(devices)
    }

    //上报数据给界面
    private fun reportData(channel: String, controlData: ControlData) {
        when (controlData.control) {
            Order.DEVICE_REGISTRATION.type -> {//设备注册、设备更新
                val device = controlData.data as Device
                //根据md5判断设备是否注册
                val localDevice = deviceDao?.findByMd5(device.md5)
                val newDevice = if (localDevice == null || localDevice.id == -1L) {
                    //本地没有数据的时候插入数据
                    deviceDao?.insert(device)
                } else {//其他的时候更新数据
                    deviceDao?.update(device)
                }
                if (newDevice != null) {
                    val md5Array = HexData.stringTobytes(newDevice.md5)
                    val command = CommandUtils.getAssignsCommand(newDevice.id.toByte(), md5Array)
                    val transferSocket = transMap[channel]
                    transferSocket?.addCommand(command)
                }
            }
            Order.RESPONSE_CARD_AUTH.type -> {
                postResult(controlData.data)
            }
        }

    }

    //回传结果
    private fun postResult(data: Any?) {
        handler.post {
            try {
                if (data != null) {
                    val dataJson = gson.toJson(data)
                    result?.success(dataJson)
                    result = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //关闭服务器
    fun closeService() {
        println("closeService")
        server?.close()
        server = null
        mExecutorService?.shutdown()
        mExecutorService = null
    }

    //获取通道ip
    private fun getId(ipAddress: String): String {
        return ipAddress.split(Regex("\\.")).last()
    }

    companion object {
        const val PORT = 10086;
    }

}