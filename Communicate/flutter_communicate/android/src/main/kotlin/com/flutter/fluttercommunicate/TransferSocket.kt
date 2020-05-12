package com.flutter.fluttercommunicate

import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.Socket
import kotlin.coroutines.CoroutineContext

/**
 * @param socket sockt通道
 * @param context 上下文
 * @param id 通道所属设备id
 * @param report 上报ui匿名函数
 */
class TransferSocket(private val socket: Socket, channel: String, report: (String, ControlData) -> Unit) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
    private var bis: BufferedInputStream? = null
    private var bos: BufferedOutputStream? = null
    private val tempBuffer = ByteArray(2048)
    private val commandHelper = CommandHelper { ControlData ->
        report(channel, ControlData)
    }

    init {
        //初始化
        try {
            bis = BufferedInputStream(socket.getInputStream())
            bos = BufferedOutputStream(socket.getOutputStream())
            //分配ip
            print("成功连接服务器")
            init()
            cycleCheck()
        } catch (e: IOException) {
            e.printStackTrace()
            close()
        }
    }

    private fun init() = launch {
        //循环读取数据
        while (isActive) {
            if (bis == null || bos == null) break
            //接受消息
            readBuffer()
        }
    }

    private fun cycleCheck() = launch {
        while (isActive) {
            //内部会阻塞 执行指令
            commandHelper.writeData(::writeData)
        }
    }


    private fun readBuffer() {
        try {
            val len = bis?.read(tempBuffer) ?: -1
            if (len > 0) {
                val readBuffer = tempBuffer.copyOfRange(0, len)
                println("readBuffer:" + HexData.hexToString(readBuffer))
                commandHelper.parserReadBuffer(readBuffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    fun addCommand(command: Command) = commandHelper.addCommand(command)

    private fun writeData(byteArray: ByteArray) {
        try {
            bos?.write(byteArray)
            bos?.flush()
            println("writeBuffer:" + HexData.hexToString(byteArray))
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    private fun close() {
        println("TransferSocket close")
        try {
            bis?.close()
            bis = null
            bos?.close()
            bos = null
            socket.close()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
    }

}