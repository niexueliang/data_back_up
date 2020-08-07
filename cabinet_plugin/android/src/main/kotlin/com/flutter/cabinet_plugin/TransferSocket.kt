package com.flutter.cabinet_plugin

import android.util.Log
import com.flutter.cabinet_plugin.entities.Command
import com.flutter.cabinet_plugin.entities.ControlData
import com.flutter.cabinet_plugin.entities.Device
import com.flutter.cabinet_plugin.entities.RFIDCommand
import com.flutter.cabinet_plugin.enums.CommandFlag
import com.flutter.cabinet_plugin.enums.DeviceType
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.util.*
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext

/**
 * @param socket sockt通道
 * @param context 上下文
 * @param id 通道所属设备id
 * @param report 上报ui匿名函数
 */
class TransferSocket(
    private val socket: Socket,
    private val channel: Int,
    private val reportToUI: (Int, ControlData) -> Unit,
    private val callBack: (Int, Boolean) -> Unit
) : CoroutineScope {
    var device: Device? = null
    var end = false
    var readThread: ReadThread? = null
    var writeThread: WriteThread? = null
    val deque = LinkedList<Any>()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
    private var helper: Helper? = null

    init {
        try {
            //循环监听队列中是否有指令需要执行
            readThread = ReadThread().apply { start() }
            //循环监听，接受消息
            writeThread = WriteThread().apply { start() }
        } catch (e: IOException) {
            e.printStackTrace()
            close()
        }
    }

    //读线程
    inner class ReadThread : Thread() {
        private val tempBuffer = ByteArray(2048)
        override fun run() {
            super.run()
            socket.getInputStream().use { `is` ->
                BufferedInputStream(`is`).use { bis ->
                    println("循环监听是否有数据返回")
                    try {
                        while (!end) {
                            val len = bis.read(tempBuffer)
                            if (len > 0) {
                                val tmpBuffer = tempBuffer.copyOfRange(0, len)
                                when (device?.type) {
                                    DeviceType.MAIN_CABINET.type, DeviceType.SUB_CABINET.type -> helper?.parserReadBuffer(
                                        tmpBuffer
                                    )
                                    DeviceType.MODE_RFID.type -> helper?.parserReadBuffer(tmpBuffer)
                                    else -> parserRegistration(tmpBuffer)
                                }
                            }
                        }
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }

    var sendTime = -1L

    //写线程
    inner class WriteThread : Thread() {
        override fun run() {
            super.run()
            socket.getOutputStream().use { os ->
                BufferedOutputStream(os).use {
                    println("监听是否有数数据等待写入")
                    try {
                        while (!end) {
                            //获取许可
                            while (deque.isEmpty()) {//没有指令的时候需要阻塞
                                val time = System.currentTimeMillis()
                                val diffTime = time - sendTime
                                if (diffTime > 4000 && device?.type != DeviceType.MODE_RFID.type) {
                                    it.write(1)
                                    it.flush()
                                    sendTime = time
                                }
                            }
                            val command = deque.removeFirst()
                            val byteArray: ByteArray? = when (device?.type) {
                                DeviceType.MAIN_CABINET.type,
                                DeviceType.SUB_CABINET.type -> {
                                    if (command is Command) {
                                        helper?.requestCommand(command)
                                    } else {
                                        null
                                    }
                                }
                                DeviceType.MODE_RFID.type -> {
                                    if (command is RFIDCommand) {
                                        helper?.requestCommand(command)
                                    } else {
                                        null
                                    }
                                }
                                else -> null
                            }
                            //写入数据
                            if (byteArray != null) {
                                println("write buffer =${HexData.hexToString(byteArray)}")
                                it.write(byteArray)
                                it.flush()
                            } else {
                                val cd = ControlData(Order.RESPONSE_ANY.type, "")
                                reportToUI(channel, cd)
                            }
                            //赋值旧的时间
                            sendTime = System.currentTimeMillis()
                        }
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    //通过被动的方式指令命令
    /**
     * true 增加成功 false增加失败
     */
    fun addCommand(any: Any): Boolean {
        return if (helper?.flag == CommandFlag.FREE) {
            deque.addLast(any)
            true
        } else {
            println("繁忙状态，无法执行指令")
            false
        }
    }

    //添加设备
    private fun parserRegistration(readBuffer: ByteArray) {
        try {
            Log.e("nil", "注册信息：${HexData.hexToString(readBuffer)}")
            val deviceList = CommandUtils.splitArrayForCommandArray(readBuffer)
            val command = deviceList
                .mapTo(arrayListOf()) { CommandUtils.getCommandFromCrcBytes(it) }
                .find { it.control == Order.DEVICE_REGISTRATION.type }
            if (command != null && command.data.isNotEmpty()) {
                //设备注册
                val dataArray = command.data
                //设备类型
                val type = dataArray[0].toInt()
                //格子相关数组
                val cellCount = dataArray[1].toInt()
                //mac地址
                val md5Hex = HexData.hexToString(dataArray.copyOfRange(2, dataArray.size))
                //返回数据
                device = Device("", md5Hex, type, cellCount, channel)
                //当是RFID模块注册的时候,需要实时初始化
                if (type == DeviceType.MODE_RFID.type) {
                    getRFIDHelper()
                } else {
                    getCommandHelper()
                }
                //通道建立
                callBack(channel, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRFIDHelper() = RFIDHelper(
        reportCommand = { rFIDCommand ->
            addCommand(rFIDCommand)
        },
        reportData = { controlData ->
            reportToUI(channel, controlData)
        }
    ).apply {
        helper = this
        addCommand(RFIDUtils.changeArea())
    }

    private fun getCommandHelper() = CommandHelper { controlData ->
        reportToUI(channel, controlData)
    }.apply {
        helper = this
    }

    private fun getEnumByType(type: Byte) = when (type.toInt()) {
        1 -> DeviceType.MAIN_CABINET
        2 -> DeviceType.SUB_CABINET
        3 -> DeviceType.MODE_RFID
        else -> DeviceType.UNKNOWN
    }

    //关闭通道
    private fun close() {
        println("TransferSocket close")
        try {
            socket.close()
            helper?.clear()
            end = true
            job.cancel()
            reportChannelChange(false)
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        callBack(channel, false)
    }

    /**
     * state true 连接  false 关闭
     */
    private fun reportChannelChange(state: Boolean) {
        val controlData = ControlData(control = Order.RESPONSE_CHANNEL_CHANGE.type, data = state)
        reportToUI(channel, controlData)
    }

}