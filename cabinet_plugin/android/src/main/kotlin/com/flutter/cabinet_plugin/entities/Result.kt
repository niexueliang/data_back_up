package com.flutter.cabinet_plugin.entities

/**
 * 统一的返回结果
 * code false 失败 true 通过
 * data 数据 当code为0表示错误信息  当code为1时表示返回数据
 */
class Result(val flag: Boolean, val data: Any)