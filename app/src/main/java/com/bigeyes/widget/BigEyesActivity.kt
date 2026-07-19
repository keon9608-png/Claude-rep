package com.bigeyes.widget

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen Big Eyes. Opening the app also (reliably, from the foreground)
 * starts [BigEyesWidgetService] so the home screen widget begins animating.
 */
class BigEyesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = BigEyesView(this)
        root.setBackgroundColor(Color.BLACK)
        setContentView(root)

        // Kick the widget animator to life while we're in the foreground.
        BigEyesWidgetService.start(this)
    }
}
