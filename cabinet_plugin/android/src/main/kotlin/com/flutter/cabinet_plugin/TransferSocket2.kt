package com.flutter.cabinet_plugin

import android.util.Log
import com.flutter.cabinet_plugin.entities.ControlData
import com.flutter.cabinet_plugin.entities.Device
import com.flutter.cabinet_plugin.enums.DeviceType
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.util.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.lang.Runnable
import java.net.Socket

/**
 * @param socket sockt通道
 * @param context 上下文
 * @param id 通道所属设备id
 * @param report 上报ui匿名函数
 */
class TransferSocket2(
        private val socket: Socket,
        val channel: Int,
        private val reportToUI: (Int, ControlData) -> Unit
) : Runnable {
    private val tempBuffer = ByteArray(512)
    private var helper: Helper2? = null
    private var bos: BufferedOutputStream? = null
    private var bis: BufferedInputStream? = null


    override fun run() {
        try {
            //初始化数据
            bis = BufferedInputStream(socket.getInputStream())
            bos = BufferedOutputStream(socket.getOutputStream())
            helper = getCommandHelper()
            while (socket.isConnected) {
                val readBytes = readBuffer()
                if (readBytes != null) {
                    Log.e("TransferSocket", "读取到数据=>${HexData.hexToString(readBytes)}")
                    (helper as CommandHelper2).parserReadBuffer(readBytes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    private fun readBuffer(): ByteArray? {
        try {
            val len = bis?.read(tempBuffer) ?: -1
            if (len > 0) {
                return tempBuffer.copyOfRange(0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            reportChannelChange(false)
        }
        return null
    }


    private fun getCommandHelper() = CommandHelper2(bos!!) { controlData ->
        reportToUI(channel, controlData)
    }.apply {
        helper = this
    }

    fun requestCommand(command: Any) {
        helper?.requestCommand(command)
    }

    //关闭通道
    fun close() {
        println("TransferSocket close")
        try {
            socket.close()
            helper?.clear()
            reportChannelChange(false)
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
    }

    /**
     * state true 连接  false 关闭
     */
    private fun reportChannelChange(state: Boolean) {
        val controlData = ControlData(control = Order.RESPONSE_CHANNEL_CHANGE.type, data = state)
        reportToUI(channel, controlData)
    }

}