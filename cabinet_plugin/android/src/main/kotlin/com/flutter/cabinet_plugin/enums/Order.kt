package com.flutter.cabinet_plugin.enums

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
    RFID(0x02, "身份认证"),

    /**
     * 设备发起注册请求
     */
    RESPONSE_RFID(0x82.toByte(), "身份认证响应"),

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

    //未分割到有效数据
    RESPONSE_ANY(0xFD.toByte(), "未分割到有效数据"),

    //通道断开
    RESPONSE_CHANNEL_CHANGE(0xFE.toByte(), "通道变化"),

    /////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////  个人版指令  ///////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////
    /**
     *  数据库文件上传指令
     */
    DB_FILE_UPLOAD(0x07, " 数据库文件上传请求"),

    /**
     * 数据库文件上传完成 下位机确认
     */
    RESPONSE_DB_FILE_UPLOAD(0x87.toByte(), " 数据库文件上传结束"),

    /**
     *  资源文件上传指令
     */
    RESOURCE_FILE_UPLOAD(0x08, "资源文件上传请求"),

    /**
     * 资源文件上传完成 下位机确认
     */
    RESPONSE_RESOURCE_FILE_UPLOAD(0x88.toByte(), "资源文件获取结束"),

    /**
     *  数据库文件下载指令
     */
    DB_FILE_DOWNLOAD(0x09, "数据库文件下载指令"),

    /**
     * 数据库文件下载确认 下位机确认
     */
    RESPONSE_DB_FILE_DOWNLOAD(0x89.toByte(), "数据库文件下载确认"),

    /**
     *  资源文件下载指令
     */
    RESOURCE_FILE_DOWNLOAD(0x0A, "资源文件下载指令"),

    /**
     * 资源文件下载确认 下位机确认
     */
    RESPONSE_RESOURCE_FILE_DOWNLOAD(0x8A.toByte(), "资源文件下载确认"),

    /**
     * 鉴权验证
     */
    AUTHENTICATE(0x0B.toByte(), "鉴权验证"),

    /**
     * 鉴权验证响应
     */
    RESPONSE_AUTHENTICATE(0x8B.toByte(), "鉴权验证响应"),


    /**
     * 修改鉴权
     */
    UPDATE_AUTHENTICATE(0x0C.toByte(), "修改鉴权"),

    /**
     * 修改鉴权响应
     */
    RESPONSE_UPDATE_AUTHENTICATE(0x8C.toByte(), "修改鉴权响应")
}

