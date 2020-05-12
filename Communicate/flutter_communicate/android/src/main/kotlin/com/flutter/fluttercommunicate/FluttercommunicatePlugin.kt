package com.flutter.fluttercommunicate

import android.os.Handler
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.logging.StreamHandler

public class FluttercommunicatePlugin : FlutterPlugin, MethodCallHandler {
    var socketService: SocketService? = null
    private var deviceDao: DeviceDao? = null
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        socketService = SocketService().apply {
            startService(flutterPluginBinding.applicationContext)
        }
        deviceDao = DeviceDao.getInstance(flutterPluginBinding.applicationContext)
        val channel = MethodChannel(flutterPluginBinding.binaryMessenger, "fluttercommunicate")
        channel.setMethodCallHandler(this)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        socketService?.transferData(call, result)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        socketService?.closeService()
        flutterPluginBinding = null
    }


    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "fluttercommunicate")
            channel.setMethodCallHandler(FluttercommunicatePlugin())
        }
    }

}
