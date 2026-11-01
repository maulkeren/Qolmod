package com.example.qolmod

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val pad = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#080D16"))
        }

        val info = TextView(this).apply {
            text = "QOLMOD V1\nMenu palsu untuk prank. Fiturnya tidak mengubah game apa pun."
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val start = Button(this).apply {
            text = "Mulai menu"
            setOnClickListener { startMenu() }
        }

        val stop = Button(this).apply {
            text = "Hentikan menu"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        layout.addView(info)
        layout.addView(start)
        layout.addView(stop)
        setContentView(layout)
    }

    private fun startMenu() {
        if (!Settings.canDrawOverlays(this)) {
            // Buka pengaturan "Tampil di atas aplikasi lain" untuk app ini
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
