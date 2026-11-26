package com.nuvio.app.features.cloudstream

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.AbstractComposeView
import java.util.Collections
import java.util.WeakHashMap

/**
 * Hides the windows that extensions open by themselves (donation, Telegram, Discord popups...).
 *
 * Nothing is intercepted, so extensions keep working: the extension opens its window normally,
 * and the app, which looks at the open windows several times per second while extensions can
 * reach the screen (and for a while after), makes it invisible, untouchable and unfocusable, and
 * removes its dimming. Back and taps go straight to the app; the extension still thinks its
 * window is shown, so closing it later works as usual.
 *
 * Never hidden: the app's own windows (Compose dialogs and sheets), the Cast windows, and the
 * windows that open just after the user asked for an extension's settings ([allowForAWhile]).
 * Needs Android 10 (the list of windows); on older versions popups show as before.
 */
internal object ExtensionPopupBlocker {
    private const val POLL_MS = 150L
    private const val WATCH_AFTER_UI_MS = 120_000L
    private const val ALLOW_AFTER_SETTINGS_MS = 5_000L

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var watchUntil = 0L

    @Volatile
    private var allowUntil = 0L
    private var running = false

    /** Windows already looked at: hidden ones, and the ones allowed to show. */
    private val hidden: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())
    private val allowed: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

    /** Called whenever extensions are allowed to reach the screen. */
    fun watch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        watchUntil = SystemClock.uptimeMillis() + WATCH_AFTER_UI_MS
        main.post {
            if (!running) {
                running = true
                main.post(tick)
            }
        }
    }

    /** The user opened an extension's settings: the windows that open now must show. */
    fun allowForAWhile() {
        allowUntil = SystemClock.uptimeMillis() + ALLOW_AFTER_SETTINGS_MS
        watch()
    }

    private val tick = object : Runnable {
        override fun run() {
            runCatching { step() }
            if (SystemClock.uptimeMillis() < watchUntil) {
                main.postDelayed(this, POLL_MS)
            } else {
                running = false
            }
        }
    }

    private fun step() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val now = SystemClock.uptimeMillis()
        WindowInspector.getGlobalWindowViews().forEach { view ->
            if (view in hidden || view in allowed) return@forEach
            if (!isExtensionPopup(view)) return@forEach
            if (now < allowUntil) {
                allowed += view
            } else {
                hide(view)
            }
        }
    }

    private fun isExtensionPopup(view: View): Boolean {
        if (!view.isAttachedToWindow) return false
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return false
        if (params.type <= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW ||
            params.type > WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
        ) {
            return false
        }
        return !isAppWindow(view)
    }

    /** Compose content (the app's dialogs and sheets) or the Cast windows. */
    private fun isAppWindow(view: View): Boolean {
        if (view is AbstractComposeView) return true
        if (view.javaClass.name.startsWith("androidx.mediarouter.")) return true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (isAppWindow(view.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun hide(view: View) {
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        hidden += view
        params.alpha = 0f
        params.dimAmount = 0f
        params.flags = (params.flags or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) and
            WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        view.visibility = View.INVISIBLE
        val windowManager = view.context.getSystemService(WindowManager::class.java) ?: return
        runCatching { windowManager.updateViewLayout(view, params) }
    }
}
