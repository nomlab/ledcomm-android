package com.example.torch_by_camera2

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {
    lateinit var textView: TextView
    lateinit var button: Button
    lateinit var manager: CameraManager
    lateinit var button_up: Button
    lateinit var button_down: Button
    lateinit var text_freq: TextView
    var cameraId: String = ""
    var flashOn = false
    var flashFlag = false
    var flash_freq = 1
    val h = Handler()

    private val run = object : Runnable{
        var freq = flash_freq
        override fun run(){
            manager.setTorchMode(cameraId, !flashOn)
            if (flashFlag && 0 < freq) {
                h.postDelayed(this, freq.toLong())
            }
        }

        fun add_freq(d: Int){
            freq += d
            if (freq <= 0) {freq = 0}
        }

        fun set_freq(d: Int){
            freq = d
        }

        fun get_freq(): Int {
            return freq
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)
        button_up = findViewById(R.id.button_up)
        button_down = findViewById(R.id.button_down)
        text_freq = findViewById(R.id.text_freq)

        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.registerTorchCallback(@RequiresApi(Build.VERSION_CODES.M)
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                super.onTorchModeChanged(id, enabled)

                id.also { cameraId = it }
                flashOn = enabled
            }
        }, null)

        button.setOnClickListener {
            flashFlag = !flashFlag
            val flashStatus = if (flashFlag) "点灯" else "消灯"
            textView.text = "ライトの状態：$flashStatus"
            if (flashFlag) {
                h.post(run)
            }else{
                h.removeCallbacks(run)
                manager.setTorchMode(cameraId, false)
            }

        }

        button_up.setOnClickListener {
            run.add_freq(1)
            text_freq.text = "${run.get_freq()}"
        }

        button_down.setOnClickListener {
            run.add_freq(-1)
            text_freq.text = "${run.get_freq()}"
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun switchLight() {
        manager.setTorchMode(cameraId, !flashOn)
        val flashStatus = if (!flashOn) "点灯" else "消灯"
        val ff = if (flashFlag) "1" else "0"

        textView.text = "ライトの状態：$flashStatus $ff"
    }

    fun freq_plus_ten(view: View) {
        run.add_freq(10)
        text_freq.text = "${run.get_freq()}"
    }
    fun freq_minus_ten(view: View) {
        run.add_freq(-10)
        text_freq.text = "${run.get_freq()}"
    }
    fun freq_plus_hund(view: View){
        run.add_freq(100)
        text_freq.text = "${run.get_freq()}"
    }
    fun freq_minus_hund(view: View){
        run.add_freq(-100)
        text_freq.text = "${run.get_freq()}"
    }
    fun freq_set_1000(view: View){
        run.set_freq(1000)
        text_freq.text = "${run.get_freq()}"
    }

}