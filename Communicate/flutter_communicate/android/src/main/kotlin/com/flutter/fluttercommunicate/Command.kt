package com.flutter.fluttercommunicate


import java.io.Serializable

/**
 * 指令交互，用于各层之间数据的传输
 * head 头部数据数组 4位
 * equipmentId 设备id
 * control 控制指令
 * count 有效数据长度
 * data 数据数组
 */
data class Command(
        val equipmentId: Byte,
        val control: Byte,
        val data: ByteArray = byteArrayOf(),
        val head: ByteArray = Constant.headBytes
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Command
        if (equipmentId != other.equipmentId) return false
        if (control != other.control) return false
        if (!data.contentEquals(other.data)) return false
        if (!head.contentEquals(other.head)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = equipmentId.toInt()
        result = 31 * result + control
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + head.contentHashCode()
        return result
    }

}