package com.flutter.cabinet_plugin.finger

import android.util.Log
import com.flutter.cabinet_plugin.entities.Result
import com.flutter.cabinet_plugin.util.HexData
import kotlinx.coroutines.*
import java.io.IOException
import java.security.InvalidParameterException
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.xor

/**
 * 容量是150枚指纹
 */
class CapacitanceHelper(private val responseAction: (Result) -> Unit) : SerialHelper(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
    private val job = SupervisorJob()
    private var _userId: Int = -1
    private var _userType: Byte = 1
    private var middleInput: Int = 0


    //开启指定端口
    fun openPort() {
        this.port = "/dev/ttyS4"
        this.setBaudRate("115200")
        try {
            this.open()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidParameterException) {
            e.printStackTrace()
        }
        Log.e("finger", "openPort over")
    }

    //关闭端口
    fun closePort() {
        stopSend()
        close()
        job.cancel()
    }


    //下发请求
    fun requestCommand(command: Byte) = launch {
        //休眠100ms防止指令发送太快造成指令丢失
        delay(500)
        try {
            val bytes = when (command) {
                COMMAND_01 -> {//录入指纹1 起始
                    getImage1()
                }
                COMMAND_02 -> {//录入指纹2 中间
                    getImage2()
                }
                COMMAND_03 -> {//录入指纹3 结束
                    getImage3()
                }
                COMMAND_04 -> {//删除指定用户
                    clearById()
                }
                COMMAND_05 -> {//清空所有用户
                    clear()
                }
                COMMAND_0B -> {//1：1 根据id比对
                    matchFingerById()
                }
                COMMAND_0C -> {//1：n 全文检索
                    matchFinger()
                }
                COMMAND_2B -> {//获取用户信息
                    getUserMessage()
                }
                COMMAND_47 -> {//获取空闲id
                    getFreeUserId()
                }
                else -> null
            }
            if (bytes != null) {
                Log.e("request", "send=>${HexData.hexToString(bytes)}")
                send(bytes)
            } else {
                Log.e("request", "错误指令")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun responseCommand(commandList: List<ByteArray>) {
        if (commandList.isNotEmpty()) {
            val command = commandList[0]
            Log.e("response", "command_size=${commandList.size}")
            Log.e("response", "command=${HexData.hexToString(command)}")
            when (command[1]) {
                COMMAND_01 -> {
                    val result = parserResult(command[4])
                    if (result.flag) {
                        //发起第一次中间按工序注册
                        requestCommand(COMMAND_02)
                    } else {
                        responseAction(result)
                    }
                }
                COMMAND_02 -> {
                    val result = parserResult(command[4])
                    if (result.flag) {
                        //发起第一次中间按工序注册
                        if (middleInput < 4) {
                            requestCommand(COMMAND_02)
                        } else {
                            requestCommand(COMMAND_03)
                        }
                    } else {
                        //中间过程出现异常,返回结果
                        responseAction(result)
                    }
                }
                COMMAND_03 -> {
                    val result = parserResult(command[4])
                    //注册结束
                    responseAction(result)
                }
                COMMAND_04 -> {
                    if (commandList.isNotEmpty()) {
                        val ackData = commandList[0]
                        Log.e("response", "删除指定用户=${HexData.hexToString(ackData)}")
                    }
                }
                COMMAND_05 -> {
                    if (commandList.isNotEmpty()) {
                        val ackData = commandList[0]
                        Log.e("response", "清空用户=${HexData.hexToString(ackData)}")
                    }
                }
                COMMAND_0B -> {
                    if (commandList.size == 1) {
                        val ackData = commandList[0]
                        Log.e("response", "1:1匹配指纹结果=${HexData.hexToString(ackData)}")
                    }
                }
                COMMAND_0C -> {
                    if (commandList.size == 1) {
                        val ackData = commandList[0]
                        Log.e("response", "1:N匹配指纹结果=${HexData.hexToString(ackData)}")
                    }
                }
                COMMAND_2B -> {
                    if (commandList.size == 2) {
                        val data = commandList[1]
                        Log.e("response", "用户数据=${HexData.hexToString(data)}")
                    } else {
                        Log.e("response", "用户数据不存在")
                    }

                }
                COMMAND_47 -> {
                    val result = parserResult(command[4])
                    if (result.flag) {
                        val addressArray = command.copyOfRange(2, 4)
                        val userId = arrayToShort(addressArray)
                        setUserId(userId)
                        Log.e("response", "第一个空闲的用户号=$userId")
                        //执行第一次指纹录入
                        requestCommand(COMMAND_01)
                    } else {
                        responseAction(result)
                    }
                }
                else -> {

                }
            }
        } else {
            Log.e("receive", "未获取到有效数据")
        }
    }

    fun setUserId(id: Int) {
        this._userId = id
    }

    fun setUserType(type: Int) {
        this._userType = type.toByte()
    }

    //获取指定范围内首个空闲的用户号
    private fun getUserMessage(): ByteArray {
        //指令1
        val dataArray = byteArrayOf(COMMAND_2B).plus(byteArrayOf(0, 0, 0, 0))
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    /**
     * 查询编号从1-150
     */
    private fun getFreeUserId(): ByteArray {
        //指令1
        val dataArray1 = byteArrayOf(COMMAND_47).plus(shortToArray(4)).plus(byteArrayOf(0x00, 0x00))
        val chk1 = chkByte(dataArray1)
        val command1 = byteArrayOf(COMMAND_FLAG).plus(dataArray1).plus(chk1).plus(COMMAND_FLAG)
        val dataArray2 = shortToArray(0x01).plus(shortToArray(0x96))
        val chk2 = chkByte(dataArray2)
        val command2 = byteArrayOf(COMMAND_FLAG).plus(dataArray2).plus(chk2).plus(COMMAND_FLAG)
        return command1.plus(command2)
    }

    //录入指纹1 开始
    private fun getImage1(): ByteArray {
        val dataArray =
                byteArrayOf(COMMAND_01).plus(shortToArray(_userId)).plus(_userType).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    //录入指纹2 中间
    private fun getImage2(): ByteArray {
        middleInput++
        val dataArray = byteArrayOf(COMMAND_02).plus(shortToArray(_userId)).plus(_userType).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    //录入指纹3 结束
    private fun getImage3(): ByteArray {
        val dataArray =
                byteArrayOf(COMMAND_03).plus(shortToArray(_userId)).plus(_userType).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }


    //1：1检索指纹
    private fun matchFingerById(): ByteArray {
        val dataArray = byteArrayOf(COMMAND_0B).plus(shortToArray(_userId)).plus(0).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    //1：N检索 指纹
    private fun matchFinger(): ByteArray {
        val dataArray = byteArrayOf(COMMAND_0C).plus(shortToArray(0)).plus(0).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }


    private fun clear(): ByteArray {
        val dataArray = byteArrayOf(COMMAND_05).plus(shortToArray(0)).plus(0).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    private fun clearById(): ByteArray {
        val dataArray = byteArrayOf(COMMAND_04).plus(shortToArray(_userId)).plus(0).plus(0)
        val chk = chkByte(dataArray)
        return byteArrayOf(COMMAND_FLAG).plus(dataArray).plus(chk).plus(COMMAND_FLAG)
    }

    override fun onDataReceived(comRecData: ComBean?) {
        val receiveBuffer = comRecData?.bRec
        if (receiveBuffer != null) {
            val commands = checkByteArray(receiveBuffer)
            if (commands.isEmpty()) Log.e("receive", "没有解析到数据")
            responseCommand(commands)
        } else {
            Log.e("receive", "收到空数据")
        }
    }

    /**
     * 指令说明
     * 0 0XF5 头
     * 1 command 指令
     * 3456 为具体的数据位
     * 7 0xF5 尾
     */
    private fun checkByteArray(byteArray: ByteArray): List<ByteArray> {
        val result = arrayListOf<ByteArray>()
        val len = byteArray.size
        var position = 0
        while (true) {
            //寻找
            val start = findCommandFlag(position, byteArray)
            if (start == len) break
            val end = findCommandFlag(start + 1, byteArray)
            if (end == len) break
            val chk = byteArray[end - 1]
            //校验数据是否满足chk
            val chkArray = byteArray.copyOfRange(start + 1, end - 1)
            val checkNum = chkByte(chkArray)
            //修改位置偏移
            position = end
            if (chk == checkNum) {//构成一个指令
                //将数据假如list
                result.add(byteArray.copyOfRange(start, end + 1))
                position += 1
            } else {
                //如果无法构成一个指令，则将end flag作为起始位置
                println("normal error")
            }
        }
        return result
    }

    private fun findCommandFlag(position: Int, byteArray: ByteArray): Int {
        var index = position
        val len = byteArray.size
        while (index < len) {
            if (byteArray[index] == COMMAND_FLAG) break
            index++
        }
        return index
    }

    //指令校验 由2-6位得异或
    private fun chkByte(chkArray: ByteArray): Byte {
        var chk = chkArray[0]
        for (i in 1 until chkArray.size) {
            chk = chk.xor(chkArray[i])
        }
        return chk
    }

    //ByteArray转short
    private fun arrayToShort(b: ByteArray) = b[0].toInt().shl(8) or b[1].toInt() and 0xff

    //值转化为2字节数组
    private fun shortToArray(checkNum: Int): ByteArray {
        val result = ByteArray(2)
        result[1] = (checkNum shr 0).toByte()
        result[0] = (checkNum shr 8).toByte()
        return result
    }

    private fun parserResult(resultCode: Byte): Result {
        return when (resultCode) {
            ACK_SUCCESS -> {
                Result(true, "操作成功")
            }
            ACK_FAIL -> {
                Result(false, "操作失败")
            }
            ACK_FULL -> {
                Result(false, "指纹数据库已满")
            }
            ACK_NOUSER -> {
                Result(false, "无此用户")
            }
            ACK_USER_OCCUPIED -> {
                Result(false, "此id用户已存在")
            }
            ACK_USER_EXIST -> {
                Result(false, "用户已存在")
            }
            ACK_TIMEOUT -> {
                Result(false, "采集超时")
            }
            ACK_TIMEOUT_finger -> {
                Result(false, "采集图像 无指纹按压 需要重复发送")
            }
            else -> {
                Result(false, "未知错误")
            }
        }
    }


    companion object {

        //指令
        val COMMAND_FLAG: Byte = 0XF5.toByte()

        //用户权限
        const val AUTHORITY_1 = 0x01
        const val AUTHORITY_2 = 0x02
        const val AUTHORITY_3 = 0x03

        //结果值
        const val ACK_SUCCESS: Byte = 0x00                //操作成功
        const val ACK_FAIL: Byte = 0x01                   //操作失败
        const val ACK_FULL: Byte = 0x04                   //指纹数据库已满
        const val ACK_NOUSER: Byte = 0x05                 //无此用户
        const val ACK_USER_OCCUPIED: Byte = 0x06          //此id用户已存在
        const val ACK_USER_EXIST: Byte = 0x07             //用户已存在
        const val ACK_TIMEOUT: Byte = 0x08                //采集超时
        const val ACK_TIMEOUT_finger: Byte = 0x09         //采集图像 无指纹按压 需要重复发送

        //指令command
        const val COMMAND_01: Byte = 0X01 //初次添加
        const val COMMAND_02: Byte = 0X02 //二次添加 可以省略 或者执行4次
        const val COMMAND_03: Byte = 0X03 //末次添加
        const val COMMAND_04: Byte = 0X04 //删除指定用户
        const val COMMAND_05: Byte = 0X05 //删除所有用户
        const val COMMAND_09: Byte = 0X09 //获取用户总数
        const val COMMAND_0B: Byte = 0X0B //1:1比对
        const val COMMAND_0C: Byte = 0X0C //1:N比对
        const val COMMAND_0A: Byte = 0X0A //获取用户权限
        const val COMMAND_21: Byte = 0X21 //波特率设置
        const val COMMAND_23: Byte = 0X23 //采集图像并提取特征值
        const val COMMAND_24: Byte = 0X24 //采集图像并上传
        const val COMMAND_28: Byte = 0X28 //设置对比等级
        const val COMMAND_2B: Byte = 0X2B //获取用户信息
        const val COMMAND_42: Byte = 0X42 //上传特征值与DSP模块数据库指纹比对1：1
        const val COMMAND_43: Byte = 0X43 //上传特征值与DSP模块数据库指纹比对1：N
        const val COMMAND_31: Byte = 0X31 //下载DSP数据库内的特征值
        const val COMMAND_44: Byte = 0X44 //上传特征值与采集指纹比对
        const val COMMAND_47: Byte = 0X47 //指定范围首个未注册用户号
    }

}