package com.zacharee1.boredsigns.services

import android.app.AlarmManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.Dev2Widget
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader


class Dev2Service : Service() {
    companion object {
        var CPU = 60F
        var GPU = 60F
        var BATT = 60F
    }

    private var isRunning = true

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        isRunning = true
        startListening()
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning = false
    }

    private fun startListening() {
        CPU = getCpuTemp()
        GPU = getGpuTemp()
        BATT = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)).getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 60).toFloat() / 10

        Utils.sendWidgetUpdate(this, Dev2Widget::class.java, null)

        if (isRunning) {
            Handler(Looper.getMainLooper()).postDelayed({
                startListening()
            }, 1000)
        }
    }

    private fun getCpuTemp(): Float {
        try {
            val p = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp")
            p.waitFor()
            val reader = BufferedReader(InputStreamReader(p.inputStream))

            val line = reader.readLine()

            return line.toFloat() / 1000.0f

        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0f
        }
    }

    private fun getGpuTemp(): Float {
        try {
            val p = Runtime.getRuntime().exec("sh")
            val os = DataOutputStream(p.outputStream)

            os.writeBytes("cat /sys/class/thermal/thermal_zone11/temp\n")
            os.flush()

            os.writeBytes("exit\n")
            os.flush()

            p.waitFor()
            os.close()

            val reader = BufferedReader(InputStreamReader(p.inputStream))

            val line = reader.readLine()

            return line.toFloat() / 10.0f

        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0f
        }
    }
}
