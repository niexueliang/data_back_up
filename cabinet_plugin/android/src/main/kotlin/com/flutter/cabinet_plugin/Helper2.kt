package com.flutter.cabinet_plugin

import com.flutter.cabinet_plugin.enums.CommandFlag

interface Helper2 {
    var flag: CommandFlag

    fun parserReadBuffer(byteArray: ByteArray)

    fun writeBuffer(byteArray: ByteArray)

    fun requestCommand(command: Any)

    fun clear()
}