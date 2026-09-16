package com.focuslock.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.focuslock.app.databinding.ActivityBlockBinding
import kotlinx.coroutines.*

class BlockActivity : AppCompatActivity() {

    private lateinit var b: ActivityBlockBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener {
            val i = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(i)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        scope.launch {
            while (isActive) {
                val left = Prefs.unlockAt(this@BlockActivity) - System.currentTimeMillis()
                if (left <= 0) {
                    finish()
                    return@launch
                }
                val s = left / 1000
                b.tvTimer.text = String.format(
                    java.util.Locale.US, "%02d:%02d:%02d",
                    s / 3600, (s % 3600) / 60, s % 60
                )
                delay(1000)
            }
        }
    }

    override fun onPause() { super.onPause(); scope.coroutineContext.cancelChildren() }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}