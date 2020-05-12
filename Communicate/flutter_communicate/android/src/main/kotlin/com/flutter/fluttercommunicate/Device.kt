package com.flutter.fluttercommunicate

/**
 * 设备相关信息
 * id 设备自增值
 * name 设备名
 * md5 该设备的物理唯一值
 * type 设备格子数
 */

data class Device(
        val name: String,
        val md5: String,
        val type: Int, //柜子类型 柜子数量  0主柜  1副柜
        val cellCount: Int,//格子数量
        var id: Long = 0
)