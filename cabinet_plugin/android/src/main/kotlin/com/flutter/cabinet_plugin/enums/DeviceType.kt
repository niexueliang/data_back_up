package com.flutter.cabinet_plugin.enums

enum class DeviceType(val type: Int) {
    MODE_RFID(1),
    MAIN_CABINET(2),
    SUB_CABINET(3),
    UNKNOWN(-1)
}