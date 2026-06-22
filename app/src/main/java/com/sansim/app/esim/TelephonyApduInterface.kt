package com.sansim.app.esim

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import android.util.Log
import java.lang.reflect.Method

/**
 * Telephony API 适配器 - 通过系统隐藏 API 访问 eSIM
 * 需要 Root + 系统特权权限（Magisk 模块）
 * 绕过 ARA-M 白名单限制
 */
class TelephonyApduInterface(
    private val tm: TelephonyManager,
    private val slotId: Int = 0,
    private val portId: Int = 0
) {
    companion object {
        const val TAG = "TelephonyApdu"
        
        // 隐藏 API 反射方法
        private val iccOpenLogicalChannelByPort: Method by lazy {
            TelephonyManager::class.java.getMethod(
                "iccOpenLogicalChannelByPort",
                Int::class.java, Int::class.java, String::class.java, Int::class.java
            )
        }
        
        private val iccCloseLogicalChannelByPort: Method by lazy {
            TelephonyManager::class.java.getMethod(
                "iccCloseLogicalChannelByPort",
                Int::class.java, Int::class.java, Int::class.java
            )
        }
        
        private val iccTransmitApduLogicalChannelByPort: Method by lazy {
            TelephonyManager::class.java.getMethod(
                "iccTransmitApduLogicalChannelByPort",
                Int::class.java, Int::class.java, Int::class.java, Int::class.java,
                Int::class.java, Int::class.java, Int::class.java, Int::class.java,
                String::class.java
            )
        }
    }
    
    private var channelHandle: Int = -1
    
    /**
     * 打开逻辑通道
     * @param aid AID (十六进制字符串)
     * @return 通道号，失败返回 -1
     */
    fun openLogicalChannel(aid: String): Int {
        return try {
            Log.d(TAG, "Opening logical channel: slot=$slotId, port=$portId, aid=$aid")
            
            val response = iccOpenLogicalChannelByPort.invoke(
                tm, slotId, portId, aid, 0
            ) as IccOpenLogicalChannelResponse
            
            if (response.status == IccOpenLogicalChannelResponse.STATUS_NO_ERROR && 
                response.channel != IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
                channelHandle = response.channel
                Log.d(TAG, "Channel opened: $channelHandle")
                channelHandle
            } else {
                Log.e(TAG, "Failed to open channel: status=${response.status}")
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "openLogicalChannel failed", e)
            -1
        }
    }
    
    /**
     * 关闭逻辑通道
     */
    fun closeLogicalChannel() {
        if (channelHandle < 0) return
        
        try {
            Log.d(TAG, "Closing channel: $channelHandle")
            iccCloseLogicalChannelByPort.invoke(tm, slotId, portId, channelHandle)
            channelHandle = -1
        } catch (e: Exception) {
            Log.e(TAG, "closeLogicalChannel failed", e)
        }
    }
    
    /**
     * 发送 APDU 命令
     * @param cla CLA 字节
     * @param ins INS 字节
     * @param p1 P1 字节
     * @param p2 P2 字节
     * @param p3 P3 字节
     * @param data 数据 (十六进制字符串)
     * @return 响应数据 (十六进制字符串)，失败返回 null
     */
    fun transmitApdu(
        cla: Int, ins: Int, p1: Int, p2: Int, p3: Int, data: String?
    ): String? {
        if (channelHandle < 0) {
            Log.e(TAG, "No open channel")
            return null
        }
        
        return try {
            Log.d(TAG, "Transmit APDU: cla=$cla ins=$ins p1=$p1 p2=$p2 p3=$p3 data=$data")
            
            val result = iccTransmitApduLogicalChannelByPort.invoke(
                tm, slotId, portId, channelHandle,
                cla, ins, p1, p2, p3, data
            ) as? String
            
            Log.d(TAG, "APDU response: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "transmitApdu failed", e)
            null
        }
    }
    
    /**
     * 发送原始 APDU 命令
     * @param apdu 完整 APDU 命令 (十六进制字符串)
     * @return 响应数据 (十六进制字符串)
     */
    fun transmitRawApdu(apdu: String): String? {
        if (apdu.length < 10) return null
        
        // 解析 APDU: CLA INS P1 P2 P3 [Data]
        val cla = apdu.substring(0, 2).toInt(16)
        val ins = apdu.substring(2, 4).toInt(16)
        val p1 = apdu.substring(4, 6).toInt(16)
        val p2 = apdu.substring(6, 8).toInt(16)
        val p3 = apdu.substring(8, 10).toInt(16)
        val data = if (apdu.length > 10) apdu.substring(10) else null
        
        return transmitApdu(cla, ins, p1, p2, p3, data)
    }
    
    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            // 尝试调用隐藏 API 检查是否可用
            iccOpenLogicalChannelByPort != null
        } catch (e: Exception) {
            false
        }
    }
}
