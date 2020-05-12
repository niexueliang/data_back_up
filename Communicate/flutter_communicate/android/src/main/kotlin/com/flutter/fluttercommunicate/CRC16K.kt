package com.flutter.fluttercommunicate

import java.lang.Exception

object CRC16K {
    //抽取crc数组中的数据数组
    fun extractSource(crcData: ByteArray): ByteArray {
        if (crcData.size < 2) {
            throw Exception("数组的长度小于2")
        }
        return crcData.copyOfRange(0, crcData.size - 2)
    }

    //生成数组的校验码
    fun generateCRC16(data: ByteArray): Int {
        val len = data.size
        var crc = 0xFFFF//16位
        for (pos in 0 until len) {
            crc = if (data[pos] < 0) {
                crc xor data[pos].toInt() + 256 // XOR byte into least sig. byte of
            } else {
                crc xor data[pos].toInt() // XOR byte into least sig. byte of crc
            }
            for (i in 8 downTo 1) { // Loop over each bit
                if (crc and 0x0001 != 0) { // If the LSB is set
                    crc = crc shr 1 // Shift right and XOR 0xA001
                    crc = crc xor 0xA001
                } else { // Else LSB is not set
                    crc = crc shr 1 // Just shift right
                }
            }
        }
        return crc
    }

    //根据长度生成数组的校验码
    fun generateCRC16(data: ByteArray, size: Int): Int {
        if (size > data.size) {
            throw IndexOutOfBoundsException()
        }
        val source = data.copyOfRange(0, size)
        return generateCRC16(source)
    }

    //输出校验码的校验数组
    //本次CRC16校验 高位在前，低位在后
    fun exportCRCArray(crc: Int): ByteArray {
        val b0 = (crc and 0xff).toByte()
        val b1 = (crc shr 8 and 0xff).toByte()
        return byteArrayOf(b0, b1)
    }

    //生成带校验的数据
    fun createDataWithCRC(source: ByteArray): ByteArray {
        val crcCode = generateCRC16(source)
        val crcArray = exportCRCArray(crcCode)
        return source.concat(crcArray)
    }

    //匹判断数据是否校验通过
    fun checkCRCArray(crcArray: ByteArray): Boolean {
        if (crcArray.size < 2) {
            throw Exception("数组的长度小于2，无法执行crc校验")
        }
        val size = crcArray.size
        val sourceArray = crcArray.copyOfRange(0, size - 2)
        //计算校验值
        val crcValue = generateCRC16(sourceArray)
        //输出校验数组
        val exportCrcArray =
                exportCRCArray(crcValue)
        val crcArr = crcArray.copyOfRange(size - 2, size)
        return crcArr.contentEquals(exportCrcArray)
    }

    //输出数据的校验码hex值
    fun exportCrcHexString(crc: Int): String {
        var c = Integer.toHexString(crc)
        when (c.length) {
            4 -> c = c.substring(2, 4) + c.substring(0, 2)
            3 -> {
                c = "0$c"
                c = c.substring(2, 4) + c.substring(0, 2)
            }
            2 -> c = "0" + c.substring(1, 2) + "0" + c.substring(0, 1)
        }
        return c
    }

    //合并数组
    private fun ByteArray.concat(arr: ByteArray): ByteArray {
        val fSize = this.size
        val sSize = arr.size
        val concatArr = ByteArray(fSize + sSize)
        System.arraycopy(this, 0, concatArr, 0, fSize)
        System.arraycopy(arr, 0, concatArr, fSize, sSize)
        return concatArr
    }
}
