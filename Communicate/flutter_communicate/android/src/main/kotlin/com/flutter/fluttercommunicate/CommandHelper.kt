package com.flutter.fluttercommunicate

import android.util.Log
import java.util.concurrent.LinkedBlockingDeque

class CommandHelper(private val report: (ControlData) -> Unit) {
    private val deque = LinkedBlockingDeque<Command>()

    //从读取的底层buffer中获取指令
    fun parserReadBuffer(byteArray: ByteArray) {
        //分割指令
        val result = CommandUtils.splitArrayForCommandArray(byteArray)
        if (result.isNullOrEmpty()) return
        //解析数据
        result.forEach { data ->
            //从crc校验数组中获取指令
            val command = CommandUtils.getCommandFromCrcBytes(data)
            //插入头部，首先执行
            addCommand(command)
        }
    }


    fun writeData(writeBuffer: (ByteArray) -> Unit) {
        //阻塞
        val command = deque.takeFirst()
        val writeBytes = parserCommand(command)
        if (writeBytes != null) {
            writeBuffer(writeBytes)
        }
    }

    fun addCommand(command: Command) {
        try {
            deque.addFirst(command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //响应指令
    private fun parserCommand(command: Command): ByteArray? {
        when (command.control) {
            Order.ASSIGNS_IDS.type -> {
                return CommandUtils.getCrcBytesFromCommand(command)
            }
            Order.DEVICE_REGISTRATION.type -> {
                //心跳模式提供注册
                if (command.data.contentEquals(Constant.okBytes)) {//心跳返回确认
                    //必须要注册，才执行查询
                    println("注册成功")
                } else {
                    addRegisterDevice(command.data)
                }
            }
            Order.CARD_AUTH.type -> {
                return CommandUtils.getCrcBytesFromCommand(command)
            }
            Order.RESPONSE_CARD_AUTH.type -> {
                if (command.data.contentEquals(Constant.okBytes)) {
                    println("等待响应rfid识别")
                } else {
                    val cardArrays = command.data
                    val rfidList = filterRFID(cardArrays)
                    //分割 筛选 标签
                    report(ControlData(Order.RESPONSE_CARD_AUTH.type, rfidList))
                }
            }
            Order.QUERY.type -> {

            }
            Order.RESPONSE_QUERY.type -> {

            }
            Order.CONTROL.type -> {

            }
            Order.RESPONSE_CONTROL.type -> {

            }
            else -> {
                println("未知指令")
            }
        }
        return null
    }

    //添加设备
    private fun addRegisterDevice(data: ByteArray) {
        try {
            //柜子数量
            val type = data[0]
            //格子相关数组
            val cellCount = data[1]
            //mac地址
            val md5Array = data.copyOfRange(2, data.size)
            println("设备${HexData.hexToString(md5Array)}请求注册,设备类型=${type},设备格子数=$cellCount}")
            //返回数据
            val device = Device("", HexData.hexToString(md5Array), type.toInt(), cellCount.toInt())
            report(ControlData(Order.DEVICE_REGISTRATION.type, device))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun filterRFID(datas: ByteArray): List<String> {
        val rfidSet = HashSet<String>()
        try {
            var index = 0
            while (true) {
                if (index + 24 <= datas.size) {
                    val start = datas[index]
                    val end = datas[index + 23]
                    if (start == FLAG_START && end == FLAG_END) {
                        val temp = datas.copyOfRange(index, index + 24)
                        //从第二字节开始取22个字节求和  校验是否是RFID帧
                        val crcBytes = temp.copyOfRange(1, 1 + 21);
                        val count = crcBytes.fold(0) { a, t ->
                            a + t.toInt().and(0xFF)
                        }
                        val low = count and 0xFF
                        val verifyCode = temp[22].toInt().and(0xFF)
                        index += if (low == verifyCode) {
                            //校验通过 抽取RFID
                            val rfidArray = temp.copyOfRange(9, 9 + 12)
                            val rfid = HexData.hexToString(rfidArray)
                            rfidSet.add(rfid)
                            //校验通过 有效偏移
                            24
                        } else {
                            1
                        }
                    } else {
                        index += 1
                    }
                } else {
                    //不能再分割出有效的RFID直接退出循环
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val resultList = rfidSet.toList()
        Log.e("rfid", "识别到RFID个数=${resultList.size}")
        resultList.forEach {
            Log.e("rfid", "RFID=$it")
        }
        return resultList
    }

    companion object {
        //START END 必须闭合 形成一条完整的RFID帧
        const val FLAG_START: Byte = 0XAA.toByte()
        const val FLAG_END: Byte = 0XDD.toByte()
    }
}