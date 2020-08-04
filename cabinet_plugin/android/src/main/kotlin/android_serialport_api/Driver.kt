package android_serialport_api

import android.util.Log
import java.io.File
import java.util.*

class Driver(val mDriverName: String, val mDeviceRoot: String) {
    private val TAG = "Driver"
    fun getDevices(): Vector<File> {
        val mDevices = Vector<File>()
        val dev = File("/dev")
        val files = dev.listFiles()
        var i = 0
        while (i < files.size) {
            if (files[i].absolutePath.startsWith(mDeviceRoot)) {
                Log.d(TAG, "Found new device: " + files[i])
                mDevices.add(files[i])
            }
            i++
        }
        return mDevices
    }
}