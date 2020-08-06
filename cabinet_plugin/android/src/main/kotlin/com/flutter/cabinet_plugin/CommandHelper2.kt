package com.flutter.cabinet_plugin

import android.util.Log
import com.flutter.cabinet_plugin.entities.*
import com.flutter.cabinet_plugin.enums.CommandFlag
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.util.CommandUtils
import com.flutter.cabinet_plugin.util.Constants.okBytes
import com.flutter.cabinet_plugin.util.CoverUtils
import com.flutter.cabinet_plugin.util.HexData
import com.flutter.cabinet_plugin.util.bytesToInt
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

/**
 * @param channel socket编号
 * @param writeToSocket 向底层写数据
 * @param reportToUI 向上层写数据
 */
class CommandHelper2(val bos: BufferedOutputStream, val reportData: (ControlData) -> Unit) : CoroutineScope, Helper2 {
    private val job = SupervisorJob()
    private val localBuffer = CopyOnWriteArrayList<Byte>()
    private var startTime = -1L
    private var endTime = -1L
    private var order: Byte = -1
    private var downloadFile: File? = null
    override var flag: CommandFlag = CommandFlag.FREE

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    //从读取的底层buffer中获取指令
    override fun parserReadBuffer(byteArray: ByteArray) {
        when (order) {
            Order.DB_FILE_DOWNLOAD.type -> {

            }
            Order.RESOURCE_FILE_DOWNLOAD.type -> {

            }
            else -> {
                localBuffer.addAll(byteArray.toList())
            }
        }
        endTime = System.currentTimeMillis()
    }

    override fun requestCommand(command: Any) {
        try {
            if (command !is Command) {
                reportData(ControlData(Order.RESPONSE_ANY.type, Result(false, "无效的指令")))
            } else {
                flag = CommandFlag.BUSY
                order = command.control
                when (command.control) {
                    Order.QUERY.type -> {
                        //查询数据
                        writeBuffer(CommandUtils.getCrcBytesFromCommand(command))
                    }
                    Order.CONTROL.type -> {
                        //控制锁开启之后的查询
                        writeBuffer(CommandUtils.getCrcBytesFromCommand(command))
                    }
                    Order.DB_FILE_UPLOAD.type -> {
                        val file = File("$sdPath${File.separator}db.back")
                        val sizeArray = CoverUtils.intToByteBig(file.length().toInt())
                        val newCommand = CommandUtils.uploadDbFileCommand(0x01, sizeArray)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        Log.e("helper", "commandHex=${HexData.hexToString(commandArray)}")
                        writeBuffer(commandArray)

                        //传输文件
                        file.forEachBlock(768) { bytes, len ->
                            Log.e("helper", "data_len=$len")
                            if (len == 768) {
                                writeBuffer(bytes)
                            } else {
                                writeBuffer(bytes.copyOfRange(0, len))
                            }
                        }

                        //传输完成
                        val overCommand = CommandUtils.transOver(0x01)
                        val overArray = CommandUtils.getCrcBytesFromCommand(overCommand)
                        Log.e("helper", "overHex=${HexData.hexToString(overArray)}")
                        writeBuffer(overArray)
                    }
                    Order.RESOURCE_FILE_UPLOAD.type -> {
                        val file = File("$sdPath${File.separator}resource.back")
                        val sizeArray = CoverUtils.intToByteBig(file.length().toInt())
                        val newCommand = CommandUtils.uploadResourceCommand(0x01, sizeArray)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        Log.e("helper", "commandHex=${HexData.hexToString(commandArray)}")
                        writeBuffer(commandArray)

                        //传输文件
                        file.forEachBlock(768) { bytes, len ->
                            Log.e("helper", "data_len=$len")
                            if (len == 768) {
                                writeBuffer(bytes)
                            } else {
                                writeBuffer(bytes.copyOfRange(0, len))
                            }
                        }

                        //传输完成
                        val overCommand = CommandUtils.transOver(0x01)
                        val overArray = CommandUtils.getCrcBytesFromCommand(overCommand)
                        Log.e("helper", "overHex=${HexData.hexToString(overArray)}")
                        writeBuffer(overArray)
                    }
                    Order.DB_FILE_DOWNLOAD.type -> {
                        downloadFile = File("$sdPath${File.separator}db_${System.currentTimeMillis()}.back")
                        val newCommand = CommandUtils.downloadDbFile(command.channel)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        writeBuffer(commandArray)
                    }
                    Order.RESPONSE_RESOURCE_FILE_DOWNLOAD.type -> {
                        downloadFile = File("$sdPath${File.separator}resource_${System.currentTimeMillis()}.back")
                        val newCommand = CommandUtils.downloadResourceFile(command.channel)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        writeBuffer(commandArray)
                    }
                    Order.AUTHENTICATE.type -> {
                        val contentArray = "123456".toByteArray()
                        val newCommand = CommandUtils.authenticate(command.channel, contentArray)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        writeBuffer(commandArray)
                    }
                    Order.UPDATE_AUTHENTICATE.type -> {
                        val contentArray = "abcdef".toByteArray()
                        val newCommand = CommandUtils.updateAuthenticate(command.channel, contentArray)
                        val commandArray = CommandUtils.getCrcBytesFromCommand(newCommand)
                        writeBuffer(commandArray)
                    }
                }
                when (command) {
                    Order.DB_FILE_DOWNLOAD.type -> {

                    }
                    Order.RESOURCE_FILE_DOWNLOAD.type -> {

                    }
                    else -> {
                        //解析结果
                        parserForSeconds()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            reportData(ControlData(Order.RESPONSE_ANY.type, Result(false, "异常：${e.message}")))
        }
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
            responseCommand(Command(Order.RESPONSE_ANY.type, Order.RESPONSE_ANY.type))
        }
    }

    private fun downloadDbFile(byteArray: ByteArray) {
        downloadFile?.writeBytes(byteArray)
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

    //解析读取指令
    private fun parserForSeconds() = launch {
        Log.e("nil", "解析数据=====>parserForSeconds")
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

    override fun writeBuffer(byteArray: ByteArray) {
        bos.write(byteArray)
        bos.flush()
    }

    companion object {
        //翻译
        var sdPath = ""
    }

}