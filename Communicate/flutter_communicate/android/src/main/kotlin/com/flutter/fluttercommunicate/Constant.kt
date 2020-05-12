package com.flutter.fluttercommunicate

object Constant {
    //头
    val headBytes = byteArrayOf(0x51, 0x44, 0x54, 0x47)

    //收到数据
    val okBytes = byteArrayOf(0x4F, 0x4B)

    //主柜
    val CABINET_MAIN: Byte = 1

    //从柜
    val CABINET_OTHER: Byte = 2

    //身份认证
    val PORT_CARD: Byte = 0
    val PORT_SCAN: Byte = 1
}