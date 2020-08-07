package com.flutter.cabinet_plugin.util

import com.flutter.cabinet_plugin.entities.ControlData
import com.flutter.cabinet_plugin.entities.RFIDCommand
import com.flutter.cabinet_plugin.enums.CommandFlag
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.enums.SocketType
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

class RFIDHelper(val reportCommand: (RFIDCommand) -> Unit, val reportData: (ControlData) -> Unit) :
        CoroutineScope, Helper {
    override var flag: CommandFlag = CommandFlag.FREE

    private val job = SupervisorJob()
    private var tmpCommand: RFIDCommand? = null
    private var localAntenna: Byte = -1
    private var initFlag: SocketType = SocketType.INIT

    private val localBuffer = CopyOnWriteArrayList<Byte>()
    private var startTime = -1L
    private var endTime = -1L
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    //请求数据
    override fun requestCommand(command: Any): ByteArray? {
        this.tmpCommand = command as RFIDCommand
        val result = if (initFlag == SocketType.OVER) {
            val antenna = command.cellId
            if (antenna != localAntenna) {
                localAntenna = command.cellId
                //开启天线
                RFIDUtils.getByteArrayFromCommand(RFIDUtils.openChannel(antenna))
            } else {
                //执行2s等待
                RFIDUtils.getByteArrayFromCommand(command)
            }
        } else {
            RFIDUtils.getByteArrayFromCommand(command)
        }
        //等待读取
        parserForSeconds()
        return result
    }

    //解析buffer
    override fun parserReadBuffer(byteArray: ByteArray) {
        localBuffer.addAll(byteArray.toList())
        endTime = System.currentTimeMillis()
    }

    //清空标志
    override fun clear() {
        job.cancel()
        localAntenna = -1
        localBuffer.clear()
    }

    //响应数据
    private fun responseCommand(commandList: List<RFIDCommand>) {
        if (commandList.isEmpty()) {
            print("没有分割到有效数据")
            reportData(ControlData(Order.RESPONSE_ANY.type, ""))
        } else {
            //正在初始化数据
            when (commandList.sortedByDescending { it.command }[0].command) {
                0x07.toByte() -> {
                    initFlag = SocketType.RATE
                    //区域切换成功，设置功率
                    reportCommand(RFIDUtils.changeRate())
                }
                0xB6.toByte() -> {
                    //设置功率成功，启用增益
                    initFlag = SocketType.GAIN
                    reportCommand(RFIDUtils.changeGain())
                }
                0xF0.toByte() -> {
                    //初始化完成
                    initFlag = SocketType.OVER
                }
                0xA8.toByte() -> {
                    //通道切换之后判断是否有指令发送
                    tmpCommand?.let { reportCommand(it) }
                            ?: reportData(ControlData(Order.RESPONSE_ANY.type, ""))
                }
                0x22.toByte() -> {
                    val rFIDList = commandList.filter { it.command == 0x22.toByte() }
                            .mapNotNull { splitToRFID(it.data) }.toSet().toList()
                    println("分割到有效数据=${rFIDList.size}")
                    rFIDList.forEach {
                        println("RFID=$it")
                    }
//                    testReport(rFIDList)
                    reportData(ControlData(tmpCommand?.command ?: -1, rFIDList))
                    tmpCommand = null
                }
                0xFF.toByte() -> {
                    println("异常情况")
                    reportData(ControlData(tmpCommand?.command ?: -1, arrayListOf<String>()))
                }
            }
        }
    }

    //分割RFID
    private fun splitToRFID(byteArray: ByteArray): String? {
        return if (byteArray.size > 5) {
            HexData.hexToString(byteArray.copyOfRange(3, byteArray.size - 2))
        } else {
            null
        }
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
        val commandList = parserByteArray(localBuffer.toByteArray())
        flag = CommandFlag.FREE
//        print("commandList=$commandList")
        responseCommand(commandList)
        localBuffer.clear()
    }

    //从buffer中解析出COMMAND
    private fun parserByteArray(byteArray: ByteArray): List<RFIDCommand> {
        return RFIDUtils
                .getRFIDByteArrayForBuffer(byteArray)
                .mapTo(arrayListOf()) {
                    RFIDUtils.getCommandForByteArray(it)
                }
    }

}