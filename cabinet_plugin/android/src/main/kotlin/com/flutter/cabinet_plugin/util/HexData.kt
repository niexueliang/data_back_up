package com.flutter.cabinet_plugin.util

object HexData {
    private const val HEXES = "0123456789ABCDEF"
    fun hexToString(data: ByteArray): String {
        val hex = StringBuilder(2 * data.size)
        for (element in data) {
            val high = element.toInt().and(0xFF).shr(4)
            val low = element.toInt().and(0x0F)
            hex.append(HEXES[high]).append(HEXES[low])
        }
        return hex.toString()
    }

    fun stringToBytes(hexString: String): ByteArray {
        val data = ByteArray(hexString.length / 2)
        for (i in hexString.indices step 2) {
            val character = hexString.substring(i, i + 2).toInt(16).toByte()
            data[i / 2] = character
        }
        return data
    }

    fun hex4digits(id: String): String {
        if (id.length == 1) return "000$id"
        if (id.length == 2) return "00$id"
        return if (id.length == 3) "0$id" else id
    }
}