package com.flutter.cabinet_plugin.util

import android.Manifest

object Constants {

    //1 SOCKET  2 SOCKET SERVER
    var PLUGIN_TYPE = 2
    const val PLUGIN_SOCKET = 1
    const val PLUGIN_SERVER = 2

    //读写SD卡权限
    const val READ_WRITE_CODE = 100
    const val READ_CODE = 98
    const val WRITE_CODE = 99
    const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

    //设置页面页面返回码
    const val REQUEST_CODE_WRITE_SETTINGS = 1

    //usb相关权限
    const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
    const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
    const val ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION"

    //文件选择页面返回码
    const val FILE_SELECTOR_CODE = 10

    /////////////////////////////////////指令相关///////////////////////////////////////
    //头
    val headBytes = byteArrayOf(0x51, 0x44, 0x54, 0x47)

    //硬件返回收到控制指令
    val okBytes = byteArrayOf(0x4F, 0x4B)

    //主柜
    const val CABINET_MAIN: Byte = 1

    //从柜
    const val CABINET_OTHER: Byte = 2

    //身份认证
    const val PORT_CARD: Byte = 0
    const val PORT_SCAN: Byte = 1

    //RFID标志
    const val RFID_START: Byte = 0XAA.toByte()
    const val RFID_END: Byte = 0XDD.toByte()

    //socket端口
    const val PORT = 10086
}