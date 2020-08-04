package com.flutter.cabinet_plugin.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import com.flutter.cabinet_plugin.util.Constants.ACTION_USB_ATTACHED
import com.flutter.cabinet_plugin.util.Constants.ACTION_USB_DETACHED
import com.flutter.cabinet_plugin.util.Constants.ACTION_USB_PERMISSION

/**
 * 监控usb的插拔情况，授权情况
 */
class UsbReceiver(private val context: Context, private val usbCallBack: UsbCallBack) :
        BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_USB_PERMISSION -> usbPermission(intent)
            ACTION_USB_ATTACHED -> usbCallBack.usbAttach()
            ACTION_USB_DETACHED -> usbCallBack.usbDetached()
        }
    }

    //usb授权
    private fun usbPermission(intent: Intent) {
        val extras = intent.extras
        val granted = extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED) ?: false
        usbCallBack.hasPermission(granted)
    }

    //注册监听
    fun register() {
        println("register")
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_USB_PERMISSION)
        intentFilter.addAction(ACTION_USB_DETACHED)
        intentFilter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(this, intentFilter)
    }

    //取消注册
    fun unregister() {
        println("unregister")
        context.unregisterReceiver(this)
    }


}