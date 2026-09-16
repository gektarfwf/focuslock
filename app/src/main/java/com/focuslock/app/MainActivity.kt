package com.focuslock.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.focuslock.app.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickJob: Job? = null
    private var selectedMinutes = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickApps.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        b.btnStart.setOnClickListener { startLock() }
        b.btnStop.setOnClickListener { stopLock() }

        b.slider.addOnChangeListener { _, value, _ ->
            selectedMinutes = value.toInt()
            b.tvDuration.text = formatDuration(selectedMinutes)
        }
        b.slider.value = 30f
        b.tvDuration.text = formatDuration(30)

        b.btnA11y.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        b.btnOwner.setOnClickListener { showOwnerHelp() }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        startTick()
    }

    override fun onPause() { super.onPause(); tickJob?.cancel() }

    private fun refreshUi() {
        val owner = Blocker.isDeviceOwner(this)
        b.tvOwnerStatus.text = if (owner) "Device Owner: ДА" else "Device Owner: НЕТ"
        b.btnOwner.isEnabled = !owner

        val active = Prefs.isActive(this)
        val blocked = Prefs.blocked(this)
        b.tvSelected.text = "Выбрано приложений: ${blocked.size}"

        if (active) {
            b.btnStart.isEnabled = false
            b.btnStop.isEnabled = true
        } else {
            b.btnStart.isEnabled = blocked.isNotEmpty()
            b.btnStop.isEnabled = false
            b.tvTimer.text = "Не активно"
        }
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val left = Prefs.unlockAt(this@MainActivity) - System.currentTimeMillis()
                if (Prefs.isActive(this@MainActivity) && left > 0) {
                    b.tvTimer.text = "Осталось: " + hms(left)
                } else if (Prefs.isActive(this@MainActivity)) {
                    refreshUi()
                }
                delay(1000)
            }
        }
    }

    private fun startLock() {
        val pkgs = Prefs.blocked(this)
        if (pkgs.isEmpty()) {
            toast("Сначала выбери приложения")
            return
        }
        val until = System.currentTimeMillis() + selectedMinutes * 60_000L
        Prefs.setUnlockAt(this, until)
        Blocker.applyAll(this, pkgs, true)
        TimerService.start(this, until)
        toast("Блокировка включена на ${formatDuration(selectedMinutes)}")
        refreshUi()
    }

    private fun stopLock() {
        val pkgs = Prefs.blocked(this)
        Blocker.applyAll(this, pkgs, false)
        Prefs.clear(this)
        TimerService.stop(this)
        refreshUi()
    }

    private fun showOwnerHelp() {
        val txt = """
            Device Owner ставится ТОЛЬКО через ADB и ТОЛЬКО на чистом устройстве.
            1) Сделай factory reset телефона.
            2) Не входи в Google-аккаунт на шаге настройки.
            3) Включи отладку по USB, подключи к ПК.
            4) Собери APK и установи: adb install -r app-debug.apk
            5) Затем:
[16.09.2026 4:17] Точка и.: adb shell dpm set-device-owner com.focuslock.app/.FocusDeviceAdminReceiver
            6) Открой FocusLock — статус должен стать ДА.
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Device Owner")
            .setMessage(txt)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun hms(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    private fun formatDuration(min: Int): String {
        return if (min < 60) "$min мин" else "${min / 60} ч ${min % 60} мин"
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}