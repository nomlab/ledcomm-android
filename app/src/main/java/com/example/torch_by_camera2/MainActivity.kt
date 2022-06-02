package com.example.torch_by_camera2

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi

import android.net.wifi.WifiManager


class MainActivity : AppCompatActivity() {
    lateinit var editText_send_string: EditText
    lateinit var textView: TextView
    lateinit var button: Button
    lateinit var button_get_SSID: Button
    lateinit var manager: CameraManager
    lateinit var text_freq: TextView
    var cameraId: String = ""
    var flashOn = false
    var flashFlag = false
    var flash_freq = 1
    val h = Handler()

    var sig_idx = 0
    var str = ""

    // dec2bin ： 10進数の数値をバイナリ形式のデータに変換
    private fun dec2bin(dec: Int): List<Int> {
        var n: Int = dec
        val bin = mutableListOf<Int>()
        while (n > 0) {
            bin.add(0, n % 2)
            n /= 2
        }
        Log.d("dec2bin", "bin size : ${bin.size}")
        // 隙間を0で埋める
        while (bin.size < 8) {
            bin.add(0, 0)
        }

        Log.d("dec2bin", "bin : $bin")

        return bin
    }

    // str2data ： 文字列データをバイナリ形式のデータに変換
    private fun str2data(str: String): MutableList<Int> {
        val data = mutableListOf<Int>()
        for (char in str) {
            data.addAll(dec2bin(char.code))
        }

        Log.d("str2data", "data : $data")

        return data
    }

    // data2sig : 送信データ data を光信号の点滅パターンに変換
    private fun data2sig(data: List<Int>): MutableList<Boolean> {
        val sig = mutableListOf<Boolean>()  // 点滅パターンを格納する空リスト(Boolean型)を宣言
                                            // true：点灯 , false：消灯
        // リーダを格納
        // 8個分点灯パターン格納 -> 8T だけ点灯させる
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        // 4個分消灯データを格納 -> 4T だけ消灯させる
        sig.add(false)
        sig.add(false)
        sig.add(false)
        sig.add(false)
        // 送信データ data を点滅パターンに変換
        // data には0と1に変換されたデータが格納されている
        for (n in data) {
            sig.add(false)
            // 0なら1個，1なら2個分点灯データを格納 -> 0は1T，1は2T点灯させる
            for (i in 0..n) {
                sig.add(true)
            }
        }

        // トレイラを格納
        sig.add(false)
        for (i in 0..8) {
            sig.add(true)
        }

        return sig
    }

    // 定期的に処理を実行するための runnable オブジェクト：run
    // run は，sigの内容に応じてフラッシュライトを点灯 or 消灯
    // その後，run自身を T ms後に起動
    private val run = object : Runnable {
        var freq = flash_freq
        var sig = data2sig(str2data(str))
        // run が行う処理
        override fun run() {
            if (sig_idx < sig.size) {   // sig の最後まで
                manager.setTorchMode(cameraId, sig[sig_idx])    // sig[sig_idx] の内容に応じてフラッシュライトの点灯を制御
                if (flashFlag && 0 < freq) {                    // flashFlag （光信号送信フラグ）が true かつ，freq(基準時間 T) が0より大きい場合
                    h.postDelayed(this, freq.toLong())       // run 自身を freq(基準時間 T)ms 後に起動 -> Tms間隔で点灯および消灯
                }
                sig_idx++
            } else {   // sig が終わったら
                manager.setTorchMode(cameraId, false)
                sig_idx = 0
                flashFlag = false
            }
        }

        fun add_freq(d: Int) {
            freq += d
            if (freq <= 0) {
                freq = 0
            }
        }

        fun set_freq(d: Int) {
            freq = d
        }

        fun set_signal(s: String) {
            sig = data2sig(str2data(s))
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
        text_freq = findViewById(R.id.text_freq)
        editText_send_string = findViewById(R.id.editText_send_string)
        editText_send_string.textSize = 24F
        button_get_SSID = findViewById(R.id.button_get_SSID)

        val wifiManager =
            this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val w_info = wifiManager.connectionInfo
        Log.i("Sample", "SSID:" + w_info.ssid)

        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.registerTorchCallback(
            @RequiresApi(Build.VERSION_CODES.M)
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) {
                    // フラッシュライトの点灯状態が切り替わった時に実行される関数
                    super.onTorchModeChanged(id, enabled)

                    id.also { cameraId = it }
                    flashOn = enabled
                }
            }, null
        )

        button.setOnClickListener {
            flashFlag = !flashFlag
            val flashStatus = if (flashFlag) "点灯" else "消灯"
            str = editText_send_string.getText().toString()
            Log.d("MyActivity", "This is log")
            Log.d("MyActivity", str)
            run.set_signal(str)
            textView.text = "送信した文字：$str \n 送信速度：${run.get_freq()} ms"
            if (flashFlag) {
                run.set_freq(7) // 基準時間Tを7msにセット
                h.post(run)
            } else {
                h.removeCallbacks(run)
                manager.setTorchMode(cameraId, false)
            }
        }

        button_get_SSID.setOnClickListener {
            run.set_signal(str)
            editText_send_string.setText(w_info.ssid.replace("\"", ""))
        }
//
//        button_up.setOnClickListener {
//            run.add_freq(1)
//            text_freq.text = "${run.get_freq()}"
//        }
//
//        button_down.setOnClickListener {
//            run.add_freq(-1)
//            text_freq.text = "${run.get_freq()}"
//        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun switchLight() {
        manager.setTorchMode(cameraId, !flashOn)
        val flashStatus = if (!flashOn) "点灯" else "消灯"
        val ff = if (flashFlag) "1" else "0"

        textView.text = "ライトの状態：$flashStatus $ff"
    }
}