package com.flutter.cabinet_plugin

import android.os.Handler
import android.util.Log
import androidx.annotation.NonNull;
import com.flutter.cabinet_plugin.usb.SocketCallBack
import com.flutter.cabinet_plugin.usb.UsbCallBack
import com.flutter.cabinet_plugin.usb.UsbReceiver
import com.flutter.cabinet_plugin.util.Constants
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar

/** CabinetPlugin */
public class CabinetPlugin : FlutterPlugin, ActivityAware, UsbCallBack, SocketCallBack {
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var registrar: Registrar? = null

    //插件
    private var usbMonitor: EventChannel.EventSink? = null
    private var socketMonitor: EventChannel.EventSink? = null

    //对应模块
    private var backUpDelegate: BackUpDelegate? = null
    private var pickFileDelegate: PickFileDelegate? = null
    private var socketServerDelegate: SocketServerDelegate? = null
    private var socketClientDelegate: SocketClientDelegate2? = null
    private var fingerDelegate: FingerDelegate? = null

    //usb接收器
    private var usbReceiver: UsbReceiver? = null

    private val handler = Handler()
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        pluginBinding = flutterPluginBinding
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        setUp(pluginBinding?.binaryMessenger, null, activityBinding)
    }


    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) =
            clear()

    override fun onDetachedFromActivity() = clear()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
            onAttachedToActivity(binding)

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()


    override fun hasPermission(flag: Boolean) {
        backUpDelegate?.writeUsbWithPermission()
    }

    override fun usbAttach() {
        val isMount = backUpDelegate?.checkUsbMount()
        if (isMount != null) usbMonitor?.success(isMount)
    }

    override fun usbDetached() {
        val isMount = backUpDelegate?.checkUsbMount()
        if (isMount != null) usbMonitor?.success(isMount)
    }

    //socket状态变化
    override fun socketChange() = notifySocketChange()

    //清空数据
    private fun clear() {
        backUpDelegate?.let {
            activityBinding?.removeActivityResultListener(it)
            activityBinding?.removeRequestPermissionsResultListener(it)
        }
        pickFileDelegate?.let {
            activityBinding?.removeActivityResultListener(it)
            activityBinding?.removeRequestPermissionsResultListener(it)
        }
        socketServerDelegate?.cancel()
        socketClientDelegate?.cancel()
        fingerDelegate?.cancel()
        backUpDelegate = null
        activityBinding = null
        socketServerDelegate = null
    }

    //初始化
    private fun setUp(
            messenger: BinaryMessenger?,
            registrar: Registrar?,
            activityBinding: ActivityPluginBinding?
    ) {
        if (registrar != null) this.registrar = registrar
        //文件备份器
        backUpDelegate = when {
            registrar != null -> {
                BackUpDelegate(activity = registrar.activity()).also {
                    registrar.addActivityResultListener(it)
                    registrar.addRequestPermissionsResultListener(it)
                }
            }
            activityBinding != null -> {
                BackUpDelegate(activity = activityBinding.activity).also {
                    activityBinding.addActivityResultListener(it)
                    activityBinding.addRequestPermissionsResultListener(it)
                }
            }
            else -> null
        }
        //文件选择器
        pickFileDelegate = when {
            registrar != null -> {
                PickFileDelegate(activity = registrar.activity()).also {
                    registrar.addActivityResultListener(it)
                    registrar.addRequestPermissionsResultListener(it)
                }
            }
            activityBinding != null -> {
                PickFileDelegate(activity = activityBinding.activity).also {
                    activityBinding.addActivityResultListener(it)
                    activityBinding.addRequestPermissionsResultListener(it)
                }
            }
            else -> null
        }
        if (Constants.PLUGIN_TYPE == Constants.PLUGIN_SOCKET) {
            //创建socket
            socketClientDelegate = when {
                registrar != null -> {
                    Log.e("nil", "SocketClientDelegate for registrar")
                    SocketClientDelegate2(registrar.context(), this)
                }
                activityBinding != null -> {
                    Log.e("nil", "SocketClientDelegate for activityBinding")
                    SocketClientDelegate2(activityBinding.activity, this)
                }
                else -> null
            }
        } else {
            //创建secoketserver
            socketServerDelegate = when {
                registrar != null -> {
                    Log.e("nil", "socketDelegate for registrar")
                    SocketServerDelegate(registrar.context(), this)
                }
                activityBinding != null -> {
                    Log.e("nil", "socketDelegate for activityBinding")
                    SocketServerDelegate(activityBinding.activity, this)
                }
                else -> null
            }
        }

        //指纹模块
        fingerDelegate = when {
            registrar != null -> {
                Log.e("nil", "fingerDelegate for registrar")
                FingerDelegate(registrar.context())
            }
            activityBinding != null -> {
                Log.e("nil", "fingerDelegate for activityBinding")
                FingerDelegate(activityBinding.activity)
            }
            else -> null
        }
        //启动socket客户端
        socketClientDelegate?.start()
        ///启动server服务
        socketServerDelegate?.start()

        //启动指纹
        fingerDelegate?.start()
        //文件选择
        initPickDocument(messenger)
        //usb监听
        initUsbMonitor(messenger)
        //socket监听
        initSocketMonitor(messenger)
        if (Constants.PLUGIN_TYPE == Constants.PLUGIN_SOCKET) {
            //SocketClient操作
            initSocketClientMethodChannel(messenger)
        } else {
            //socketServer操作
            initSocketServerMethodChannel(messenger)
        }
        //指纹操作
        Log.e("finger", "initFingerMethodChannel::::::::")
        initFingerMethodChannel(messenger)
    }

    //文件选择
    private fun initPickDocument(messenger: BinaryMessenger?) =
            MethodChannel(messenger, "cabinet.plugin.document")
                    .setMethodCallHandler { call, result ->
                        when (call.method) {
                            "getPlatformVersion" -> {
                                result.success("Android ${android.os.Build.VERSION.RELEASE}")
                            }
                            "backUp" -> {
                                val params = call.arguments as? Map<*, *>
                                val type = params?.get("backType") as? Int
                                var name = params?.get("backName") as? String
                                val content = params?.get("content") as? String
                                println("backupData:::$type:::$name:::$content")
                                if (content == null) {
                                    return@setMethodCallHandler
                                }
                                if (name.isNullOrEmpty()) {
                                    name = "${System.currentTimeMillis()}.back"
                                }
                                if (type == null) {
                                    println("备份方式为必须传项")
                                } else {
                                    backUpDelegate?.writeFile(result, type, name, content)
                                }
                            }
                            "pickFile" -> {
                                try {
                                    val params = call.arguments as? List<*>
                                    val paramsArray = params?.map { it.toString() }?.toTypedArray()
                                    println("params::$params")
                                    pickFileDelegate?.pickFile(result, paramsArray)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            else -> result.error("100", "未实现方法", "")
                        }
                    }

    //usb监听
    private fun initUsbMonitor(messenger: BinaryMessenger?) =
            EventChannel(messenger, "cabinet.plugin.usb_monitor")
                    .setStreamHandler(object : EventChannel.StreamHandler {
                        //EventChannel
                        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                            usbMonitor = events
                            //usb监听通道
                            if (activityBinding?.activity != null) {
                                usbReceiver = UsbReceiver(activityBinding!!.activity, this@CabinetPlugin)
                                usbReceiver?.register()
                            }
                            //主动上报一次usb是否插入
                            usbMonitor?.success(backUpDelegate?.checkUsbMount())
                        }

                        override fun onCancel(arguments: Any?) {
                            usbReceiver?.unregister()
                        }

                    })

    //socket监听
    private fun initSocketMonitor(messenger: BinaryMessenger?) =
            EventChannel(messenger, "cabinet.plugin.socket_monitor")
                    .setStreamHandler(object : EventChannel.StreamHandler {
                        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                            socketMonitor = events
                            notifySocketChange()
                        }

                        override fun onCancel(arguments: Any?) {
                            socketMonitor?.endOfStream()
                        }

                    })

    //socket管控
    private fun initSocketServerMethodChannel(messenger: BinaryMessenger?) =
            MethodChannel(messenger, "cabinet.plugin.socket_server_channel")
                    .setMethodCallHandler { call, result ->
                        socketServerDelegate?.transferData(call, result)
                    }

    private fun initSocketClientMethodChannel(messenger: BinaryMessenger?) =
            MethodChannel(messenger, "cabinet.plugin.socket_client_channel")
                    .setMethodCallHandler { call, result ->
                        socketClientDelegate?.transferData(call, result)
                    }

    //指纹通道
    private fun initFingerMethodChannel(messenger: BinaryMessenger?) =
            MethodChannel(messenger, "cabinet.plugin.finger_channel")
                    .setMethodCallHandler { call, result ->
                        fingerDelegate?.transferData(call, result)
                    }

    //通过socket变化
    private fun notifySocketChange() {
        //防止子线程更新ui造成程序崩溃
        handler.post {
            if (Constants.PLUGIN_TYPE == Constants.PLUGIN_SOCKET) {
                //初始化成功，检测是否有设备已经连接
                val channel = socketClientDelegate?.channel()
                val channelResult = if (channel != null) {
                    listOf(channel)
                } else {
                    null
                }
                socketMonitor?.success(channelResult)
            } else {
                //初始化成功，检测是否有设备已经连接
                val channels = socketServerDelegate?.channels()
                socketMonitor?.success(channels?.isNotEmpty() ?: false)
            }

        }
    }

    companion object {
        @Volatile
        private var instance: CabinetPlugin? = null


        @JvmStatic
        fun registerWith(registrar: Registrar) {
            println("registerWith")
            if (registrar.activity() == null) return
            getInstance().setUp(registrar.messenger(), registrar, null)
        }

        private fun getInstance(): CabinetPlugin {
            return instance ?: synchronized(this) {
                instance ?: CabinetPlugin().also { instance = it }
            }
        }
    }

}
