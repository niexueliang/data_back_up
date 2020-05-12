package com.flutter.fluttercommunicate

import android.content.ContentValues
import android.content.Context

class DeviceDao(context: Context) {
    private val db = DBHelper.getInstance(context)
    private val columns = arrayOf("name", "md5", "type", "cellCount", "id")
    fun insert(device: Device): Device {
        val values = ContentValues()
        values.put("name", device.name)
        values.put("md5", device.md5)
        values.put("type", device.type)
        values.put("cellCount", device.cellCount)
        val databases = db.writableDatabase
        val id = databases.replace(DBHelper.TB_NAME, null, values)
        device.id = id
        return device
    }

    fun update(device: Device): Device {
        val values = ContentValues()
        values.put("type", device.type)
        values.put("cellCount", device.cellCount)
        val databases = db.writableDatabase
        val id = databases.update(DBHelper.TB_NAME, values, "md5 = ?", arrayOf(device.md5))
        device.id = id.toLong()
        return device
    }

    fun getDevices(): List<Device> {
        val result = arrayListOf<Device>()
        val databases = db.writableDatabase
        val cursor = databases.query(DBHelper.TB_NAME, columns, null, null, null, null, null, null)
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            val md5 = cursor.getString(1)
            val type = cursor.getInt(2)
            val cellCount = cursor.getInt(3)
            val id = cursor.getLong(4)
            result.add(Device(name, md5, type, cellCount, id))
        }
        cursor.close()
        return result;
    }


    fun findByMd5(data: String): Device? {
        var device: Device? = null
        val databases = db.writableDatabase
        val cursor = databases.query(DBHelper.TB_NAME, columns, "md5 = ?", arrayOf(data), null, null, null)
        if (cursor.count > 0 && cursor.moveToFirst()) {
            val name = cursor.getString(0)
            val md5 = cursor.getString(1)
            val type = cursor.getInt(2)
            val cellCount = cursor.getInt(3)
            val id = cursor.getLong(4)
            device = Device(name, md5, type, cellCount, id)
        }
        cursor.close()
        return device
    }

    companion object {
        @Volatile
        private var instance: DeviceDao? = null
        fun getInstance(context: Context): DeviceDao {
            return instance ?: synchronized(LazyThreadSafetyMode.SYNCHRONIZED) {
                instance ?: DeviceDao(context).apply { instance = this }
            }
        }
    }
}