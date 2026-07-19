package com.bigeyes.widget

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/** Immersive full-screen Big Eyes that follow your finger and the phone's tilt. */
class BigEyesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = BigEyesView(this)
        root.setBackgroundColor(Color.BLACK)
        setContentView(root)
    }
}
