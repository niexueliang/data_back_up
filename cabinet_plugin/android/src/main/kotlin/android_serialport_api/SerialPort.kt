package android_serialport_api

import android.util.Log

import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SerialPort @Throws(SecurityException::class, IOException::class)
constructor(device: File, baudrate: Int, flags: Int) {

    /*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
    private var mFd: FileDescriptor?
    private var mFileInputStream: FileInputStream
    private var mFileOutputStream: FileOutputStream

    // Getters and setters
    val inputStream: InputStream
        get() = mFileInputStream

    val outputStream: OutputStream
        get() = mFileOutputStream

    init {
        /* Check access permission */
//        if (!device.canRead() || !device.canWrite()) {
//            try {
//                /* Missing read/write permission, trying to chmod the file */
//                val su: Process = Runtime.getRuntime().exec("/system/xbin/su")
//                val cmd = ("chmod 666 " + device.absolutePath + "\n" + "exit\n")
//                Log.e("nil", "修改路径权限=${device.absolutePath}")
//                su.outputStream.write(cmd.toByteArray())
//                if (su.waitFor() != 0 || !device.canRead() || !device.canWrite()) {
//                    throw SecurityException()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                throw SecurityException()
//            }
//        }
        mFd = open(device.absolutePath, baudrate, flags)
        if (mFd == null) {
            Log.e(TAG, "native open returns null")
            throw IOException()
        }
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
        Log.e(TAG, "SerialPort开启成功……")
    }


    // JNI

    external fun close()

    private external fun open(path: String, baudrate: Int, flags: Int): FileDescriptor

    companion object {

        private val TAG = "SerialPort"

        //        // JNI
//        @JvmStatic
//        external fun open(path: String, baudrate: Int, flags: Int): FileDescriptor
        init {
            Log.e(TAG, "loadLibrary..............")
            System.loadLibrary("serial_port")
        }
    }
}
