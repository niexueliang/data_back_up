package flutter.databackup

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import com.github.mjdev.libaums.UsbMassStorageDevice

/**
 * 监控usb的插拔情况，授权情况
 */
class UsbReceiver(private val usbReceiverCallBack: UsbCallBack) :
        BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_USB_PERMISSION -> {

                val extras = intent.extras
                val granted = extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED) ?: false
                usbReceiverCallBack.usbDeviceChecked(granted)
            }
            ACTION_USB_ATTACHED -> {
                usbReceiverCallBack.usbAttached()
            }
            ACTION_USB_DETACHED -> {
                usbReceiverCallBack.usbDetached()
            }
        }
    }

    fun register(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_USB_PERMISSION)
        intentFilter.addAction(ACTION_USB_DETACHED)
        intentFilter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(this, intentFilter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    /**
     * 用户请求usb监听的权限
     */


    /**
     * 主动暴露usb的连接状态
     */
    interface UsbCallBack {
        /**
         * 找到有效的usb设备
         */
        fun usbDeviceChecked(flag: Boolean)

        /**
         * usb连接
         */
        fun usbAttached()

        /**
         * usb断开连接
         */
        fun usbDetached()
    }

    companion object {
        const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        const val ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION"

        //请求u盘操作权限
        fun requestPermission(context: Context, device: UsbMassStorageDevice) {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val permissionIntent =
                    PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device.usbDevice, permissionIntent);
        }
    }
}