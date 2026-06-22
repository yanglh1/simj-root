package com.sansim.app.esim

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Telephony API 读卡器后端
 * 通过系统隐藏 API 访问 eSIM，绕过 ARA-M
 * 需要 Root + 系统特权权限
 */
class TelephonyReaderBackend(
    private val context: Context,
    private val slotId: Int = 0,
    private val portId: Int = 0
) : ReaderBackend {
    companion object {
        const val TAG = "TelephonyReader"
    }
    
    private var telephonyApdu: TelephonyApduInterface? = null
    private var connected = false
    
    override suspend fun connect() {
        Log.d(TAG, "connect: slot=$slotId, port=$portId")
        
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyApdu = TelephonyApduInterface(tm, slotId, portId)
        
        if (!telephonyApdu!!.isAvailable()) {
            throw IllegalStateException("Telephony API not available")
        }
        
        connected = true
        Log.d(TAG, "connect: OK")
    }
    
    override fun disconnect() {
        Log.d(TAG, "disconnect")
        telephonyApdu?.closeLogicalChannel()
        telephonyApdu = null
        connected = false
    }
    
    override fun openChannel(aid: ByteArray): Boolean {
        val aidHex = aid.joinToString("") { "%02X".format(it) }
        Log.d(TAG, "openChannel: $aidHex")
        
        val apdu = telephonyApdu ?: return false
        val channel = apdu.openLogicalChannel(aidHex)
        
        if (channel >= 0) {
            Log.d(TAG, "openChannel: OK, channel=$channel")
            return true
        }
        
        Log.e(TAG, "openChannel: failed")
        return false
    }
    
    override fun closeChannel(channel: Int) {
        Log.d(TAG, "closeChannel: $channel")
        telephonyApdu?.closeLogicalChannel()
    }
    
    override fun transmit(command: ByteArray): ByteArray {
        val cmdHex = command.joinToString("") { "%02X".format(it) }
        Log.d(TAG, "transmit: $cmdHex")
        
        val apdu = telephonyApdu ?: return byteArrayOf()
        val response = apdu.transmitRawApdu(cmdHex)
        
        if (response != null) {
            // 将十六进制字符串转换为字节数组
            val responseBytes = response.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            Log.d(TAG, "transmit response: ${responseBytes.joinToString("") { "%02X".format(it) }}")
            return responseBytes
        }
        
        Log.e(TAG, "transmit: failed")
        return byteArrayOf()
    }
    
    override fun isReady(): Boolean {
        return connected && telephonyApdu != null
    }
    
    override fun getReaderName(): String {
        return "Telephony API (slot=$slotId, port=$portId)"
    }
}
