package com.flutter.fluttercommunicate

/**
 * @param channel 标记通道 用于兼容多个socket
 */
data class ControlData(val control: Byte, val data: Any)