package com.mapbox.vision.examples

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.mapbox.vision.examples.DemoApplication.Companion.CAMERA_IP
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.math.BigDecimal
import java.net.InetAddress
import java.net.Socket

class ConnectService : Service() {

    var receiver: MessageReceiver? = null
    var connectThread: Thread? = null
    var sendThread: Thread? = null
    var receiveThreadRunning = false
    var sendThreadRunning = false

    companion object {
        private val TAG: String = "ConnectService"
        var socket: Socket? = null
    }

    interface MessageReceiver {
        fun onReceive(x: Double, y: Double, z: Double)
    }

    inner class ServiceBinder : Binder() {
        fun register(callback: MessageReceiver) {
            Log.e(TAG, "register")

            receiver = callback
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e(TAG, "onBind")
        return ServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        receiveThreadRunning = true
        sendThreadRunning = true

        connectThread = object : Thread() {
            override fun run() {
                connect(CAMERA_IP, 8080)
            }
        }
        connectThread?.start()

        sendThread = object : Thread() {
            override fun run() {
                while (sendThreadRunning) {
                    send(getGSensor().toString())
                    try {
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        sendThread?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Won't run unless it's EXPLICITLY STARTED
        Log.e(TAG, "Loc service ONSTARTCOMMAND")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        receiver = null
        receiveThreadRunning = false
        sendThreadRunning = false
    }

    fun connect(ip: String, port: Int) {
        Log.e(TAG, "connect：$ip")


        try {
            val adder = InetAddress.getByName(ip)
            socket = Socket(adder, port)
            socket?.apply {
                Log.e(TAG, "connectd")

                setReceiveBufferSize(256000)
                setTcpNoDelay(true)
                while (receiveThreadRunning) {
                    val buffer = ByteArray(1024)
                    var num = 0
                    while (receiveThreadRunning && getInputStream().read(buffer).also { num = it } != -1) {
                        val resp = ByteArray(num)
                        System.arraycopy(buffer, 0, resp, 0, num)

                        val json = JSONObject(String(resp))
                        val cmdType = json.getString("type")
                        Log.e(TAG,"receive message type:"+ cmdType)

                        if (cmdType == "getgsensordata_res") {
                            Log.e(TAG,"receive getgsensordata_res")

                            var x = 0.0
                            var y = 0.0
                            var z = 0.0
                            var temp = 0.0
                            var computdata = json.getInt("gsensordatax") shr 4
                            var negative = computdata shr 11

                            //negative 0=正數, 1=負數
                            if (negative == 0) {
                                temp = computdata.toDouble()
                                x = div(temp, 1024.0, 3)
                            } else {
                                temp = (2048 - (computdata and 0x7FF)).toDouble()
                                x = -div(temp, 1024.0, 3)
                            }

                            computdata = json.getInt("gsensordatay") shr 4
                            negative = computdata shr 11
                            if (negative == 0) {
                                temp = computdata.toDouble()
                                y = div(temp, 1024.0, 3)
                            } else {
                                temp = (2048 - (computdata and 0x7FF)).toDouble()
                                y = -div(temp, 1024.0, 3)
                            }

                            computdata = json.getInt("gsensordataz") shr 4
                            negative = computdata shr 11
                            if (negative == 0) {
                                temp = computdata.toDouble()
                                z = div(temp, 1024.0, 3)
                            } else {
                                temp = (2048 - (computdata and 0x7FF)).toDouble()
                                z = -div(temp, 1024.0, 3)
                            }
                            receiver?.onReceive(x, y, z)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            receiveThreadRunning = false
            Log.e(TAG,"==============connect failed:"+ e.toString())
        }
    }

    fun send(data: String): Boolean {
        Log.e(TAG, "send $data")

        var count = 0
        var result = false
        do {
            socket?.apply {
                try {
                    Log.e(TAG, "Send Socket to Device: $data")
                    val out = PrintWriter(BufferedWriter(OutputStreamWriter(getOutputStream())), true)
                    out.println(data)
                    result = true
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            count++
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } while (!result && count < 1) //3
        return result
    }

    fun getGSensor(): JSONObject? {
        val obj = JSONObject()
        try {
            obj.put("type", "getgsensordata")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj
    }

    private fun div(d1: Double, d2: Double, scale: Int): Double {
        val b1 = BigDecimal(java.lang.Double.toString(d1))
        val b2 = BigDecimal(java.lang.Double.toString(d2))
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).toDouble()
    }
}
