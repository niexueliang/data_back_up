package com.flutter.cabinet_plugin.util

import com.flutter.cabinet_plugin.entities.RFIDCommand
import com.flutter.cabinet_plugin.util.Constants.RFID_END
import com.flutter.cabinet_plugin.util.Constants.RFID_START

object RFIDUtils {
    fun getRFIDByteArrayForBuffer(buffer: ByteArray): List<ByteArray> {
        println("解析的字符串长度=${buffer.size}")
        val result = arrayListOf<ByteArray>()

        var index = 0
        while (true) {
            //buffer比对结束 跳出循环
            if (index >= buffer.size) break
            //起始字符
            val start = buffer[index]
            if (start == RFID_START) {//头
                //类型和指令占用2字节 数据长度占用2字节
                if (index + 2 + 2 >= buffer.size) break
                //获取数据区域
                val lenArr = buffer.copyOfRange(index + 3, index + 3 + 2)
                //计算出数据区域长度
                val len = lenArr.bytesToInt().and(0xFF)
                //数据区域占用len字节
                if (index + 2 + 2 + len >= buffer.size) break
                //待校准区域
                val checkArray = buffer.copyOfRange(index + 1, index + 1 + 4 + len)
                //校验码1字节
                if (index + 2 + 2 + len + 1 >= buffer.size) break
                val checkNum = buffer[index + 2 + 2 + len + 1]
                val arrayNum = checkArray.fold(0) { r, t -> r + t }.toByte()
                //判断数据区域校验是否通过
                if (arrayNum == checkNum) {
                    //结尾1字节
                    if (index + 2 + 2 + len + 1 + 1 >= buffer.size) break
                    val end = buffer[index + 2 + 2 + len + 1 + 1]
                    if (end == RFID_END) {
                        //匹配成功 拷贝数据
                        val commandArray =
                            buffer.copyOfRange(index, index + 2 + 2 + len + 1 + 1 + 1)
                        val isContain = result.find {
                            HexData.hexToString(it) == HexData.hexToString(commandArray)
                        }
                        if (isContain == null) {
                            println("分割出的command=${HexData.hexToString(commandArray)}")
                            result.add(commandArray)
                        }
                        index += 2 + 2 + len + 1 + 1
                    }
                }
            }
            index += 1
        }
        println("分割出RFID命令数量=${result.size}")
//        result.forEach {
//            println(HexData.hexToString(it))
//        }
        return result
    }

    fun getCommandForByteArray(commandArray: ByteArray): RFIDCommand {
        val type = commandArray[1]
        val command = commandArray[2]
        val countArray = commandArray.copyOfRange(3, 5)
        val count = countArray.bytesToInt().toByte()
        val dataArray = commandArray.copyOfRange(5, 5 + count)
        val checkSum = commandArray[5 + count]
        return RFIDCommand(type, command, countArray, dataArray, checkSum)
    }

    fun getByteArrayFromCommand(command: RFIDCommand): ByteArray {
        val result = byteArrayOf()
        return result.concat(command.start)
            .concat(command.type)
            .concat(command.command)
            .concat(command.plArray)
            .concat(command.data)
            .concat(command.checksum)
            .concat(command.end)
    }


    //返回切换区域的指令
    fun changeArea() = getCommand(0x07, byteArrayOf(0x02))

    //开启1号天线
    /**
     * @param num 天线类型
     */
    fun openChannel(num: Byte) = getCommand(0xA8.toByte(), byteArrayOf(0x00, num, 0x01))


    //设置功率
    fun changeRate(): RFIDCommand {
        return getCommand(0xB6.toByte(), byteArrayOf(0x0B, 0xB8.toByte()))
    }

    //设置增益
    fun changeGain(): RFIDCommand {
        return getCommand(0xF0.toByte(), byteArrayOf(0x04, 0x06, 0x01, 0x20))
    }

    //身份认证
    fun identification() = getCommand(0x22, byteArrayOf())

    //扫描 每次轮询10次
    fun scan(cellId: Byte) =
        getCommand(0x27, byteArrayOf(0x22, 0x00, 0x1E)).apply { this.cellId = cellId }

    private fun getCommand(command: Byte, byteArray: ByteArray, type: Byte = 0): RFIDCommand {
        val lenArray = byteArray.size.shortToBytes()
        val checkSum =
            byteArray.concat(command).concat(type).concat(lenArray).fold(0) { r, t -> r + t }
                .toByte()
        return RFIDCommand(type, command, lenArray, byteArray, checkSum)
    }
}