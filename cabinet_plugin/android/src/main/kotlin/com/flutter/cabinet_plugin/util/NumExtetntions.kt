package com.flutter.cabinet_plugin.util

//2位数组转int
fun Int.shortToBytes(): ByteArray {
    //构建长度数组
    val highByte = this.shr(8).and(0xFF).toByte()
    val lowByte = this.and(0xFF).toByte()
    return byteArrayOf(highByte, lowByte)
}

//int转2位数组
fun ByteArray.bytesToInt(): Int {
    //获取数据区域
    val highSource = this[0].toInt().and(0xFF)
    val lowSource = this[1].toInt().and(0xFF)
    //数组转数值
    return highSource.shl(8).or(lowSource.and(0xFF))
}


fun ByteArray.concat(arr: Byte): ByteArray {
    return this.concat(byteArrayOf(arr))
}


fun ByteArray.concat(arr: ByteArray): ByteArray {
    val fSize = this.size
    val sSize = arr.size
    val concatArr = ByteArray(fSize + sSize)
    System.arraycopy(this, 0, concatArr, 0, fSize)
    System.arraycopy(arr, 0, concatArr, fSize, sSize)
    return concatArr
}
