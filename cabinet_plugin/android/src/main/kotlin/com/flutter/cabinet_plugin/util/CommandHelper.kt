package com.flutter.cabinet_plugin.util

import android.util.Log
import com.flutter.cabinet_plugin.entities.*
import com.flutter.cabinet_plugin.enums.CommandFlag
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.util.Constants.okBytes
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.and

/**
 * @param channel socket编号
 * @param writeToSocket 向底层写数据
 * @param reportToUI 向上层写数据
 */
class CommandHelper(val reportData: (ControlData) -> Unit) : CoroutineScope, Helper {
    private val job = SupervisorJob()
    private val localBuffer = CopyOnWriteArrayList<Byte>()
    private var startTime = -1L
    private var endTime = -1L
    override var flag: CommandFlag = CommandFlag.FREE

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    //从读取的底层buffer中获取指令
    override fun parserReadBuffer(byteArray: ByteArray) {
        println("read_buffer->${HexData.hexToString(byteArray)}")
        localBuffer.addAll(byteArray.toList())
        endTime = System.currentTimeMillis()
    }

    override fun requestCommand(command: Any): ByteArray? {
        command as Command
        flag = CommandFlag.BUSY
        val result = when (command.control) {
            Order.QUERY.type -> {
                //查询数据
                CommandUtils.getCrcBytesFromCommand(command)
            }
            Order.CONTROL.type -> {
                //控制锁开启之后的查询
                CommandUtils.getCrcBytesFromCommand(command)
            }
            else -> null
        }
        parserForSeconds()
        return result
    }


    override fun clear() {
        job.cancel()
    }

    private fun parserCabinetMode(byteArray: ByteArray) {
        //分割指令
        val result = CommandUtils.splitArrayForCommandArray(byteArray)
        if (result.isNotEmpty()) {//解析数据
            result.forEach { data ->
                //从crc校验数组中获取指令
                val command = CommandUtils.getCommandFromCrcBytes(data)
                //插入头部，首先执行
                responseCommand(command)
            }
        } else {
            //向写一条任意指令，用于完成该次交互
            responseCommand(Command(-1, Order.RESPONSE_ANY.type))
        }
    }

    //响应socket结果
    private fun responseCommand(command: Command) {
        when (command.control) {
            Order.RESPONSE_QUERY.type -> {
                //解析数据
                val dataArray = command.data
                //解析数据
                val cellData = parserDataArray(dataArray)
                if (cellData != null) {
                    Log.e("nil", "cellData=$cellData")
                    //回传数据
                    reportData(ControlData(command.control, cellData))
                }
            }
            Order.RESPONSE_CONTROL.type -> {
                //获取控制结果
                if (command.data.contentEquals(okBytes)) {
                    reportData(ControlData(command.control, true))
                }
            }
            Order.RESPONSE_ANY.type -> {
                reportData(ControlData(command.control, ""))
            }
        }
        flag = CommandFlag.FREE
    }


    //解析数据
//    private fun parserDataArray(dataArray: ByteArray): CellData? {
//        if (dataArray.size != 8) return null
//        val stateArray = dataArray.copyOfRange(0, 2)
//        val state = stateArray.bytesToInt()
//        val pressureArray = dataArray.copyOfRange(2, 4)
//        val pressure = pressureArray.bytesToInt() / 100.0
//        val temperatureArray = dataArray.copyOfRange(4, 6)
//        val temperature = temperatureArray.bytesToInt() / 100.0
//        val humidityArray = dataArray.copyOfRange(6, 8)
//        val humidity = humidityArray.bytesToInt() / 100.0
//        return CellData(state, pressure, temperature, humidity)
//    }

    //温度 湿度 状态
    private fun parserDataArray(dataArray: ByteArray): CellData? {
        if (dataArray.size != 5) return null
        val temperatureArray = dataArray.copyOfRange(0, 2)
        val temperature = temperatureArray.bytesToInt() / 10.0
        val humidityArray = dataArray.copyOfRange(2, 4)
        val humidity = humidityArray.bytesToInt() / 10.0
        val state = dataArray[4].toInt()
        return CellData(state, 0.0, temperature, humidity)
    }

    //发送读取指令
    private fun parserForSeconds() = launch {
        //线程状态
        flag = CommandFlag.BUSY
        //同步时间
        startTime = System.currentTimeMillis()
        endTime = startTime
        while (isActive) {
            //阻塞 当200ms没有返回数据则认为数据接受完成，结束阻塞
            if (System.currentTimeMillis() - endTime > 1000) break
        }
        parserCabinetMode(localBuffer.toByteArray())
        localBuffer.clear()
    }

}