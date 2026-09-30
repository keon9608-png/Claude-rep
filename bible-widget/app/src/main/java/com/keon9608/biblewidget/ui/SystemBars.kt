package com.keon9608.biblewidget.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * 화면을 상태 표시줄·내비게이션 바 뒤까지 그린다(Android 15부터는 기본 동작이라 모든 버전을 맞춘다).
 * 여백은 [doOnSystemBarInsets]로 직접 준다.
 */
@Suppress("DEPRECATION")
internal fun Activity.drawBehindSystemBars(darkStatusIcons: Boolean = !isNightMode()) {
    val darkNavIcons = !isNightMode()
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        val status = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        val nav = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(
            (if (darkStatusIcons) status else 0) or (if (darkNavIcons) nav else 0),
            status or nav,
        )
    } else {
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (darkStatusIcons) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (darkNavIcons) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags
    }
}

/** 상태 표시줄·내비게이션 바가 차지하는 크기(px)를 알려 준다. 화면이 돌아가면 다시 불린다. */
internal fun View.doOnSystemBarInsets(block: (left: Int, top: Int, right: Int, bottom: Int) -> Unit) {
    setOnApplyWindowInsetsListener { _, insets ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            block(bars.left, bars.top, bars.right, bars.bottom)
        } else {
            @Suppress("DEPRECATION")
            block(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
        }
        insets
    }
    requestApplyInsets()
}

internal fun Activity.isNightMode(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

internal fun Activity.dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)
