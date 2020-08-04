package com.flutter.cabinet_plugin.entities

/**
 * @param  1 start 固定为0xAA
 * @param  1 type 0x00 指令  0x01 结果
 * @param  1 command 指令 0x07切换区域  0xA8切换天线  0XB6设置发射功率  0XF0设置接收解调器参数
 * @param  2 plArray 指令长度 求出的值需要转化为byte  不知道为啥要用两位？
 * @param  根据countArray计算长度  data 数据区域
 * @param 1 end 固定为0xDD
 * @param cellId 指令作用的天线号
 */
data class RFIDCommand(
        val type: Byte,
        val command: Byte,
        val plArray: ByteArray,
        val data: ByteArray,
        val checksum: Byte,
        val start: Byte = 0xAA.toByte(),
        val end: Byte = 0xDD.toByte()) {
    //默认作用与天线1
    var cellId: Byte = -1
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RFIDCommand
        if (start != other.start) return false
        if (type != other.type) return false
        if (command != other.command) return false
        if (!plArray.contentEquals(other.plArray)) return false
        if (!data.contentEquals(other.data)) return false
        if (checksum != other.checksum) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.toInt()
        result = 31 * result + type
        result = 31 * result + command
        result = 31 * result + plArray.contentHashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + checksum
        result = 31 * result + end
        return result
    }
}