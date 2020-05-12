package com.flutter.fluttercommunicate

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?, factory: CursorFactory? = null, name: String? = DB_NAME, version: Int = DB_VERSION) :
        SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TB_NAME (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,type INTEGER,cellCount TEXT,md5 TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    companion object {
        const val DB_NAME = "communicate.db"
        const val TB_NAME = "device"
        const val DB_VERSION = 1

        @Volatile
        private var instance: DBHelper? = null
        fun getInstance(context: Context): DBHelper {
            return instance ?: synchronized(LazyThreadSafetyMode.SYNCHRONIZED) {
                instance ?: DBHelper(context).apply { instance = this }
            }
        }
    }
}