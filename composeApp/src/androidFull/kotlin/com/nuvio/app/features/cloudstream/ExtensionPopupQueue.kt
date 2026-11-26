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
import java.util.WeakHashMap

/**
 * Shows the windows that extensions open by themselves (donation, Telegram, Discord popups...)
 * one at a time instead of stacked on top of each other.
 *
 * Nothing is intercepted: the extension opens its window normally. While extensions can reach
 * the screen (and a little after), the app looks at the open windows a few times per second. When
 * a second extension window appears while one is already shown, it is made invisible and
 * untouchable until the first one closes, then shown again exactly as it was. Buttons, dismiss and
 * everything else in the extension's window keep working.
 *
 * Only plain dialog windows that contain no Compose content count: the app's own dialogs and
 * sheets are Compose, menus and spinners are sub-windows, so they are never held back.
 * Needs Android 10 (the window list); on older versions popups behave as before.
 */
internal object ExtensionPopupQueue {
    private const val POLL_MS = 350L
    private const val WATCH_AFTER_UI_MS = 20_000L

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var watchUntil = 0L
    private var running = false

    /** Extension windows in the order they appeared; the first one is the one shown. */
    private val order = ArrayList<View>()

    /** Windows held back, with what they looked like before. */
    private val held = WeakHashMap<View, Saved>()

    private class Saved(val alpha: Float, val dimAmount: Float, val flags: Int)

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

    private val tick = object : Runnable {
        override fun run() {
            runCatching { step() }
            if (SystemClock.uptimeMillis() < watchUntil || order.isNotEmpty()) {
                main.postDelayed(this, POLL_MS)
            } else {
                running = false
            }
        }
    }

    private fun step() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val popups = WindowInspector.getGlobalWindowViews().filter(::isExtensionPopup)
        // Closed windows leave the queue.
        order.retainAll { view -> popups.any { it === view } }
        held.keys.retainAll { view -> popups.any { it === view } }
        // New ones join it, in the order they are found.
        popups.forEach { view -> if (order.none { it === view }) order += view }
        order.forEachIndexed { index, view -> if (index == 0) release(view) else hold(view) }
    }

    private fun isExtensionPopup(view: View): Boolean {
        if (!view.isAttachedToWindow) return false
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return false
        if (params.type <= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW ||
            params.type > WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
        ) {
            return false
        }
        return !containsCompose(view)
    }

    private fun containsCompose(view: View): Boolean {
        if (view is AbstractComposeView) return true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (containsCompose(view.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun hold(view: View) {
        if (held.containsKey(view)) return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        held[view] = Saved(params.alpha, params.dimAmount, params.flags)
        params.alpha = 0f
        params.flags = (params.flags or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) and
            WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        update(view, params)
    }

    private fun release(view: View) {
        val saved = held.remove(view) ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.alpha = saved.alpha
        params.dimAmount = saved.dimAmount
        params.flags = saved.flags
        update(view, params)
    }

    private fun update(view: View, params: WindowManager.LayoutParams) {
        val windowManager = view.context.getSystemService(WindowManager::class.java) ?: return
        runCatching { windowManager.updateViewLayout(view, params) }
    }
}
