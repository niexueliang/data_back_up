package com.flutter.cabinet_plugin.util

import com.flutter.cabinet_plugin.enums.CommandFlag

interface Helper {
    var flag: CommandFlag

    fun parserReadBuffer(byteArray: ByteArray)

    fun requestCommand(command: Any): ByteArray?

    fun clear()
}