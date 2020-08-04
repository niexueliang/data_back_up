package com.flutter.cabinet_plugin.util

import android.util.Log
import com.flutter.cabinet_plugin.entities.Command
import com.flutter.cabinet_plugin.enums.Order
import com.flutter.cabinet_plugin.util.Constants.headBytes

object CommandUtils {
    //根据command 获取写入串口的数据
    fun getCrcBytesFromCommand(command: Command): ByteArray {
        val channel = command.channel
        val control = command.control
        val dataArray = command.data
        val count = dataArray.size
        val countArray = count.shortToBytes()
        val baseArr = headBytes
                .concat(byteArrayOf(channel, control))
                .concat(countArray)
                .concat(dataArray)
        return CRC16K.createDataWithCRC(baseArr)
    }

    //从数组中获取到指令
    @Throws(Exception::class)
    fun getCommandFromCrcBytes(crcBytes: ByteArray): Command {
        //使用crc16校验数据
        val checkFlag = CRC16K.checkCRCArray(crcBytes)
        if (!checkFlag) {
            val content = "数据CRC16校验无法通过"
            throw Exception(content)
        }
        //抽取资源数据
        val source = CRC16K.extractSource(crcBytes)
        //检查是否为指令信息
        if (source.size < 7) {
            val content = "接收到数组的长度不满7位"
            throw Exception(content)
        }
        val headArr = source.copyOfRange(0, 4)
        val equipmentId = source[4]
        val control = source[5]
        //数据区域长度2个字节
        val countArray = source.copyOfRange(6, 8)
        val count = countArray.bytesToInt()
        //判定数据长度是否足够
        if (8 + count != source.size) {
            val content = "数据区域不存在数据"
            throw Exception(content)
        }
        var dataArr: ByteArray = byteArrayOf()
        if (count > 0) {
            //拷贝数据区域
            dataArr = source.copyOfRange(8, 8 + count)
        }
        //校验头部信息
        if (!headArr.contentEquals(headBytes)) {
            val exception = Exception("头部校验无法通过")
            throw exception
        }
        return Command(equipmentId, control, dataArr, headArr)
    }

    //从数组中分割出有效的数组集合
    @Throws(Exception::class)
    fun splitArrayForCommandArray(sourceByteArray: ByteArray): List<ByteArray> {
        val result = arrayListOf<ByteArray>()
        try {
            var position = 0
            val count = sourceByteArray.size
            while (true) {
                //判定是否超界
                if (position >= count) break
                //一条数据包含校验位至少有9位
                if (position + 4 > count) break
                //获取四字节头部
                val headByteArray = sourceByteArray.copyOfRange(position, position + 4)
                //头部数据匹配
                if (!headBytes.contentEquals(headByteArray)) {
                    position += 1
                    continue
                }
                //判断剩余数据是否包含一个完整的指令 4头+1设备id+1指令+2数据长度+N数据+2校验
                if (position + 10 > count) break
                //复制两个字节
                val countArray = sourceByteArray.copyOfRange(position + 6, position + 6 + 2)
                val sourceCount = countArray.bytesToInt()
                //终点位置 index = count -1
                val toIndex = position + 8 + sourceCount + 2
                if (toIndex > count) break
                val dataBytes = sourceByteArray.copyOfRange(position, toIndex)
                //校验dataBytes的crc值
                val pass = CRC16K.checkCRCArray(dataBytes)
                if (pass) {
                    position += toIndex
                    result.add(dataBytes)
                } else {
                    position += 1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("nil", "分割的数据：${result.size}")
        return result
    }

    fun getQueryCommand(channel: Byte, cellId: Byte) =
            Command(channel, Order.QUERY.type, data = byteArrayOf(cellId))

    fun getControlCommand(channel: Byte, cellId: Byte) =
            Command(channel, Order.CONTROL.type, data = byteArrayOf(cellId))

    fun uploadDbFileCommand(channel: Byte) =
            Command(channel, Order.DB_FILE_UPLOAD.type)

    fun uploadResourceCommand(channel: Byte) =
            Command(channel, Order.RESOURCE_FILE_UPLOAD.type)

    fun downloadDbFile(channel: Byte) =
            Command(channel, Order.DB_FILE_DOWNLOAD.type)

    fun downloadResourceFile(channel: Byte) =
            Command(channel, Order.RESOURCE_FILE_DOWNLOAD.type)

    fun authenticate(channel: Byte, dataArray: ByteArray) =
            Command(channel, Order.RESOURCE_FILE_DOWNLOAD.type, dataArray)

    fun updateAuthenticate(channel: Byte, dataArray: ByteArray) =
            Command(channel, Order.RESOURCE_FILE_DOWNLOAD.type, dataArray)
}
