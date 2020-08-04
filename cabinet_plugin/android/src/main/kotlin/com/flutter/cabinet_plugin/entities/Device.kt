package com.flutter.cabinet_plugin.entities

import com.flutter.cabinet_plugin.enums.DeviceType
import java.io.Serializable

/**
 * 设备相关信息
 * id 设备自增值
 * name 设备名
 * md5 该设备的物理唯一值
 * type 设备类型
 * cellCount 设备数量
 */

data class Device(
    val name: String,
    val md5: String,
    val type: Int, //柜子类型 1主柜  2副柜  3 RFID模块
    val cellCount: Int,//当type为0-1代表格子数量。  当type为2时代表模块数量，值默认为1。
    var channel: Int = -1//ip地址
) : Serializable {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Device

                if (name != other.name) return false
                if (md5 != other.md5) return false
                if (type != other.type) return false
                if (cellCount != other.cellCount) return false
                if (channel != other.channel) return false

                return true
        }

        override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + md5.hashCode()
                result = 31 * result + type.hashCode()
                result = 31 * result + cellCount
                result = 31 * result + channel
                return result
        }
}