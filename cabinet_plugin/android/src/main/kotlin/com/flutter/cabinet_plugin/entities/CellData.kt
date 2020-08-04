package com.flutter.cabinet_plugin.entities

/**
 * 格子的状态
 * @param state 锁状态 0开启 1关闭
 * @param  pressure 气压
 * @param  temperature 温度
 * @param humidity 湿度
 */
data class CellData(
    val state: Int,
    val pressure: Double,
    val temperature: Double,
    val humidity: Double
)