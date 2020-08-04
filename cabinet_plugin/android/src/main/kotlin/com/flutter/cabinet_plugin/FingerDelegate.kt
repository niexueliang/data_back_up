package com.flutter.cabinet_plugin

import android.content.Context
import android.os.Handler
import androidx.annotation.NonNull
import com.flutter.cabinet_plugin.entities.Result
import com.flutter.cabinet_plugin.finger.CapacitanceHelper
import com.google.gson.Gson
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FingerDelegate(private val context: Context) : Thread() {
    private var result: MethodChannel.Result? = null
    private val handler = Handler()
    private val g = Gson()
    private val helper = CapacitanceHelper {
        handler.post { result?.success(g.toJson(it)) }
    }

    override fun run() {
        super.run()
        helper.openPort()
    }

    fun transferData(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        this.result = result
        when (call.method) {
            "fingerInput" -> {
                //注册首先要寻找空闲的id，应该发送代码47
                helper.requestCommand(CapacitanceHelper.COMMAND_47)
            }
            "matchById" -> {
                //比对指定的指纹
                val fingerId = call.argument<Int>("fingerId")
                if (fingerId != null) {
                    helper.setUserId(fingerId)
                    helper.requestCommand(CapacitanceHelper.COMMAND_0B)
                } else {
                    result.success(Result(false, "必须指定需要匹配的指纹"))
                }
            }
            "deleteById" -> {
                //删除指定用户
                val fingerId = call.argument<Int>("fingerId")
                if (fingerId != null) {
                    helper.requestCommand(CapacitanceHelper.COMMAND_04)
                } else {
                    result.success(Result(false, "必须指定需要删除的指纹"))
                }
            }
            "clear" -> {
                //清空所有用户
                helper.requestCommand(CapacitanceHelper.COMMAND_05)
            }
            "allUser" -> {
                helper.requestCommand(CapacitanceHelper.COMMAND_2B)
            }
            else -> {

            }
        }
    }

    //清空
    fun cancel() {
        helper.closePort()
    }
}