package com.baguetteui.wtrip

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object Insets {

    /**
     * Apply status bar top inset as paddingTop to the given view (recommended: AppBarLayout).
     * This prevents toolbar title/navigation/menu from being overlapped by the status bar.
     */
    fun applyTopInsetPadding(view: View) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(
                initialLeft,
                initialTop + topInset,
                initialRight,
                initialBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}