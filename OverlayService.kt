package com.example.qolmod

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var webView: WebView? = null
    private val ui = Handler(Looper.getMainLooper())

    private val density get() = resources.displayMetrics.density
    private val screenW get() = resources.displayMetrics.widthPixels
    private val screenH get() = resources.displayMetrics.heightPixels
    private fun dp(v: Int) = (v * density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        webView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    // ---------- Notifikasi (wajib untuk foreground service) ----------
    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "QOLMOD", NotificationManager.IMPORTANCE_LOW)
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("QOLMOD berjalan")
            .setContentText("Ketuk Stop untuk menutup menu")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }

    // ---------- Jendela melayang ----------
    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            dp(COLLAPSED_DP), dp(COLLAPSED_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        val view = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setOnLongClickListener { true }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(Bridge(), "Android")
            loadUrl("file:///android_asset/menu.html")
        }

        webView = view
        wm.addView(view, params)
    }

    private fun applyLayout() {
        params.x = params.x.coerceIn(0, maxOf(0, screenW - params.width))
        params.y = params.y.coerceIn(0, maxOf(0, screenH - params.height))
        webView?.let { wm.updateViewLayout(it, params) }
    }

    // Dipanggil dari JavaScript (thread terpisah, jadi dipindah ke UI thread)
    inner class Bridge {
        @JavascriptInterface
        fun setExpanded(expanded: Boolean) {
            ui.post {
                if (expanded) {
                    params.width = minOf(dp(MENU_W_DP), screenW - dp(16))
                    params.height = minOf(dp(MENU_H_DP), (screenH * 0.75f).toInt())
                } else {
                    params.width = dp(COLLAPSED_DP)
                    params.height = dp(COLLAPSED_DP)
                }
                applyLayout()
            }
        }

        // dx, dy dalam CSS px (= dp), jadi dikali density
        @JavascriptInterface
        fun moveBy(dx: Float, dy: Float) {
            ui.post {
                params.x += (dx * density).toInt()
                params.y += (dy * density).toInt()
                applyLayout()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "qolmod"
        const val ACTION_STOP = "com.example.qolmod.STOP"
        const val COLLAPSED_DP = 64
        const val MENU_W_DP = 300
        const val MENU_H_DP = 440
    }
}
