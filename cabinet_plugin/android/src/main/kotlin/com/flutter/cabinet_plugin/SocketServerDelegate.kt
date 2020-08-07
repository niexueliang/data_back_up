package com.flutter.cabinet_plugin

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull
import com.flutter.cabinet_plugin.entities.Command
import com.flutter.cabinet_plugin.entities.ControlData
import com.flutter.cabinet_plugin.entities.Device
import com.flutter.cabinet_plugin.enums.DeviceType
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.usb.SocketCallBack
import com.flutter.cabinet_plugin.util.CommandUtils
import com.flutter.cabinet_plugin.util.Constants.PORT
import com.flutter.cabinet_plugin.util.RFIDUtils
import com.google.gson.Gson
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.net.ServerSocket

class SocketServerDelegate(private val context: Context, private val callBack: SocketCallBack) :
        Thread() {
    //flutter数据交互通道
    var result: MethodChannel.Result? = null
    var end: Boolean = false


    //通道列表
    private var transMap = hashMapOf<Int, TransferSocket>()

    private val handler = Handler()

    private val g = Gson()

    override fun run() {
        super.run()
        startService(context)
    }

    //启动监听
    private fun startService(context: Context) {
        //use实现了Closeable接口,其参数在函数结束后会自动关闭,调用其close方法,无论是否发生异常.
        try {
            ServerSocket(PORT).use { server ->
                while (!end) {
                    val socket = server.accept()
                    val address = socket.inetAddress.hostAddress
                    val channel = getId(address).toInt()
                    println("建立socket连接::channel=$channel")
                    //根据channel查找设备
                    val transferSocket = TransferSocket(socket, channel, ::reportData, ::socketChange)
                    //保存接入设备
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
                reportConnectCabinets()
            }
            "identification" -> {
                identification()
            }
            "getCabinetMainMessage" -> {
                getCabinetMainMessage()
            }
            "getCabinetCellMessage" -> {
                val md5 = call.argument<String>("md5")
                val cell = call.argument<String>("cell")
                if (md5 != null && cell != null) {
                    getCabinetCellMessage(md5, cell)
                } else {
                    throw  Exception("参数异常，请传入完整的参数")
                }
            }
            "scan" -> {
                val md5 = call.argument<String>("md5")
                val cell = call.argument<String>("cell")
                if (md5 != null && cell != null) {
                    scanning(md5, cell)
                } else {
                    throw Exception("参数异常，请传入完整的参数")
                }
            }
            "openCabinetCellDoor" -> {
                val md5 = call.argument<String>("md5")
                val cell = call.argument<String>("cell")
                if (md5 != null && cell != null) {
                    openCabinetCellDoor(md5, cell)
                } else {
                    throw  Exception("参数异常，请传入完整的参数")
                }
            }
            else -> {
            }
        }
    }

    //获取指定单元格温湿度数据
    private fun getCabinetCellMessage(md5: String, cell: String) {
        val transferSocket =
                transMap.values.find { it.device?.md5 == md5 && it.device?.type != DeviceType.MODE_RFID.type }
        val device = transferSocket?.device
        if (transferSocket != null && device != null) {
            val command =
                    CommandUtils.getQueryCommand(device.channel.toByte(), cell.toInt().toByte())
            transferSocket.addCommand(command)
        } else {
            postResult(null)
        }
    }

    private fun openCabinetCellDoor(md5: String, cell: String) {
        Log.e("nil", "开启柜门===>柜子编号=$md5")
        val transferSocket =
                transMap.values.find { it.device?.md5 == md5 && it.device?.type != DeviceType.MODE_RFID.type }
        val device = transferSocket?.device
        if (transferSocket != null && device != null) {
            val command =
                    CommandUtils.getControlCommand(device.channel.toByte(), cell.toInt().toByte())
            transferSocket.addCommand(command)
        } else {
            postResult(null)
        }
    }

    //识别rfid
    private fun identification() {
        Log.e("nil", "身份认证")
        val transferSocket = transMap.values.find { it.device?.type == DeviceType.MODE_RFID.type }
        if (transferSocket != null) {
            transferSocket.addCommand(RFIDUtils.scan(0x01))
        } else {
            postResult(null)
        }
    }

    private fun getCabinetMainMessage() {
        val transferSocket =
                transMap.values.find { it.device?.type == DeviceType.MAIN_CABINET.type }
        val device = transferSocket?.device
        if (transferSocket != null && device != null) {
            val command: Command = CommandUtils.getQueryCommand(device.channel.toByte(), 0x01)
            transferSocket.addCommand(command)
        } else {
            postResult(null)
        }
    }

    //扫描指定柜子 指定格子的rfid
    private fun scanning(md5: String, cell: String) {
        Log.e("nil", "扫描===>柜子编号=$md5:::格子编号=$cell")
        //获取配对的RFID模块
//        val transferSocket = transMap.values.find {
//            it.device?.md5 == md5 && it.device?.type == DeviceType.MODE_RFID.type
//        }
//        println("socket=$transferSocket")
//        if (transferSocket != null) {
//            val antenna = getAntennaByCell(cell.toInt())
//            transferSocket.addCommand(RFIDUtils.scan(antenna.toByte()))
//        } else {
//            postResult(null)//未找到对应RFID模块
//        }
        val transferSocket = transMap.values.find {
            it.device?.type == DeviceType.MODE_RFID.type
        }
        transferSocket?.addCommand(RFIDUtils.scan(1.toByte()))
    }

    private fun getAntennaByCell(cell: Int): Int {
        val division = cell / 4
        val mode = cell % 4
        return division + if (mode > 0) 1 else 0
    }

    private fun reportConnectCabinets() = postResult(getCabinets())


    private fun getCabinets(): List<Device> {
        return transMap.values
                .mapNotNull { it.device }
                .filter { it.type == DeviceType.MAIN_CABINET.type || it.type == DeviceType.SUB_CABINET.type }
    }

    //获取当前通道情况
    fun channels(): List<Int> {
        //返回结果
        return getCabinets().map { it.channel }
    }

    //上报数据给界面
    private fun reportData(channel: Int, controlData: ControlData) {
        when (controlData.control) {
            Order.RESPONSE_RFID.type -> {//rfid识别
                postResult(controlData.data)
            }
            Order.RESPONSE_QUERY.type -> {//查询数据结果
                postResult(controlData.data)
            }
            Order.RESPONSE_CONTROL.type -> {
                //直接回传主页，指令送达
                postResult(true)
            }
            Order.RESPONSE_ANY.type -> {
                //直接回传主页，指令送达
                postResult(null)
            }
            Order.RESPONSE_CHANNEL_CHANGE.type -> {
                val data = controlData.data as Boolean
                //判断通道的状态
                if (!data) {
                    transMap.remove(channel)
                }
                //通道建立
                handler.post { callBack.socketChange() }
            }
            0x22.toByte() -> {
                postResult(controlData.data)
            }
            0x27.toByte() -> {
                postResult(controlData.data)
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

    //获取通道ip
    private fun getId(ipAddress: String): String {
        return ipAddress.split(Regex("\\.")).last()
    }

    /**
     * 通道状态改变 当为断开的时候 需要剔除旧的通道
     */
    private fun socketChange(channel: Int, state: Boolean) {
        //判断通道的状态
        if (!state) {
            transMap.remove(channel)
        }
        callBack.socketChange()
    }

    fun cancel() {
        end = true
        this.interrupt()
    }
}