package com.flutter.fluttercommunicate

enum class Order(val type: Byte, val description: String) {
    /**
     * 向指定设备分配id
     */
    ASSIGNS_IDS(0x01, "分配ID"),

    /**
     * 设备发起注册请求
     */
    DEVICE_REGISTRATION(0x81.toByte(), "设备请求注册"),

    /**
     * 身份认证
     */
    CARD_AUTH(0x02, "身份认证"),

    /**
     * 设备发起注册请求
     */
    RESPONSE_CARD_AUTH(0x82.toByte(), "身份认证响应"),

    /**
     * 请求查询指定设备的状态
     */
    QUERY(0x03, "查询"),

    /**
     * 设备响应查询
     */
    RESPONSE_QUERY(0x83.toByte(), "响应查询"),

    /**
     *  请求控制指定的设备
     */
    CONTROL(0x06, "控制"),

    /**
     * 设备响应控制
     */
    RESPONSE_CONTROL(0x86.toByte(), "响应控制"),

}

