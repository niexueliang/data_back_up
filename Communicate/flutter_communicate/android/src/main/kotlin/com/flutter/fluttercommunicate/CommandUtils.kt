package com.flutter.fluttercommunicate

import android.util.Log
import kotlin.experimental.and

object CommandUtils {

    private fun createCommandArray(equipmentId: Byte, control: Byte, dataArray: ByteArray?): ByteArray {
        //回调转发出本次的指令
        val count = dataArray?.size ?: 0
        val countArray = count.shortToBytes()
        val baseArr = Constant.headBytes
                .concat(byteArrayOf(equipmentId, control))
                .concat(countArray)
        //计算crc校验值
        if (count > 0) {
            val concatArr = baseArr.concat(dataArray!!)
            return CRC16K.createDataWithCRC(concatArr)
        }
        return CRC16K.createDataWithCRC(baseArr)
    }

    fun getCrcBytesFromCommand(command: Command): ByteArray {
        return createCommandArray(command.equipmentId, command.control, command.data)
    }

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
        if (!headArr.contentEquals(Constant.headBytes)) {
            val exception = Exception("头部校验无法通过")
            throw exception
        }
        return Command(equipmentId, control, dataArr, headArr)
    }

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
                if (!Constant.headBytes.contentEquals(headByteArray)) {
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
        return result
    }

    //2位数组转int
    fun Int.shortToBytes(): ByteArray {
        //构建长度数组
        val highByte = this.shr(8).and(0xFF).toByte()
        val lowByte = this.and(0xFF).toByte()
        return byteArrayOf(highByte, lowByte)
    }

    //int转2位数组
    fun ByteArray.bytesToInt(): Int {
        //获取数据区域
        val highSource = this[0].toInt()
        val lowSource = this[1].toInt()
        //数组转数值
        return highSource.shl(8).or(lowSource.and(0xFF))
    }

    //合并数组
    private fun ByteArray.concat(arr: ByteArray): ByteArray {
        val fSize = this.size
        val sSize = arr.size
        val concatArr = ByteArray(fSize + sSize)
        System.arraycopy(this, 0, concatArr, 0, fSize)
        System.arraycopy(arr, 0, concatArr, fSize, sSize)
        return concatArr
    }

    private fun ByteArray.concat(byte: Byte): ByteArray {
        val arr = byteArrayOf(byte)
        return concat(arr)
    }

    fun getQueryCommand(equipmentId: Byte) = Command(equipmentId, Order.QUERY.type)

    fun getCardAuthentication(id: Byte, port: Byte) = Command(id, Order.CARD_AUTH.type, byteArrayOf(port))

    fun getAssignsCommand(equipmentId: Byte, ids: ByteArray) =
            Command(equipmentId, Order.ASSIGNS_IDS.type, ids)


}
