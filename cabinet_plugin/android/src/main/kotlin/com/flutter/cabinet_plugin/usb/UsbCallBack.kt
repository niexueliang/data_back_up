package com.flutter.cabinet_plugin.usb

/**
 * 主动暴露usb的连接状态
 */
interface UsbCallBack {
    /**
     * 找到有效的usb设备
     */
    fun hasPermission(flag: Boolean)

    //usb插入
    fun usbAttach()

    //usb拔出
    fun usbDetached()
}