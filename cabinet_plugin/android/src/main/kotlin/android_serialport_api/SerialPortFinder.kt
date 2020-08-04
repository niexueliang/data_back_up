package android_serialport_api

import android.util.Log
import java.io.FileReader
import java.io.IOException
import java.io.LineNumberReader
import java.util.*

class SerialPortFinder {
    private val TAG = "SerialPort"

    @Throws(IOException::class)
    internal fun getDrivers(): Vector<Driver> {
        val mDrivers = Vector<Driver>()
        val r = LineNumberReader(FileReader("/proc/tty/drivers"))
        while (true) {
            val l = r.readLine() ?: break
            // Issue 3:
            // Since driver name may contain spaces, we do not extract driver name with split()
            val drivername = l.substring(0, 0x15).trim { it <= ' ' }
            val w = l.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (w.size >= 5 && w[w.size - 1] == "serial") {
                Log.d(TAG, "Found new driver " + drivername + " on " + w[w.size - 4])
                mDrivers.add(Driver(drivername, w[w.size - 4]))
            }
        }
        r.close()
        return mDrivers
    }

    @Throws(IOException::class)
    fun getAllDevices(): Array<String> {
        val devices = Vector<String>()
        // Parse each driver
        val itdriv: Iterator<Driver>
        itdriv = getDrivers().iterator()
        while (itdriv.hasNext()) {
            val driver = itdriv.next()
            val itdev = driver.getDevices().iterator()
            while (itdev.hasNext()) {
                val device = itdev.next().name
                val value = String.format("%s (%s)", device, driver.mDriverName)
                devices.add(value)
            }
        }
        return devices.toTypedArray()
    }

    @Throws(IOException::class)
    fun getAllDevicesPath(): Array<String> {
        val devices = Vector<String>()
        // Parse each driver
        val itdriv: Iterator<Driver>
        itdriv = getDrivers().iterator()
        while (itdriv.hasNext()) {
            val driver = itdriv.next()
            val itdev = driver.getDevices().iterator()
            while (itdev.hasNext()) {
                val device = itdev.next().absolutePath
                devices.add(device)
            }
        }
        return devices.toTypedArray()
    }
}