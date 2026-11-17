package com.one.stocks

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** One-time welcome screen shown the first time the app is opened. */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val root = findViewById<View>(R.id.welcome_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            windowInsets
        }

        val startButton = findViewById<View>(R.id.btn_welcome_start)
        startButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            StockPreferences.setWelcomeSeen(this, true)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        if (savedInstanceState == null) {
            playEntrance(findViewById(R.id.welcome_content), findViewById(R.id.welcome_footer))
        }
    }

    private fun playEntrance(content: LinearLayout, footer: View) {
        val offset = 18f * resources.displayMetrics.density
        val views = (0 until content.childCount).map { content.getChildAt(it) } + footer
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = offset
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80L + index * 55L)
                .setDuration(420L)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()
        }
    }
}
