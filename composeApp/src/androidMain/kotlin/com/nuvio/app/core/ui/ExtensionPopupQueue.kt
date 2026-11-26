package com.nuvio.app.core.ui

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

/**
 * Shows the windows that extensions open on their own (donation, Telegram, Discord popups...)
 * one at a time instead of piling them on top of each other.
 *
 * The activity hands extensions this window manager. A window opened by an extension while
 * another extension window is on screen waits in a queue; it appears, unchanged, when the one on
 * screen closes. The extension's dialog keeps working as usual: its buttons, its dismiss and its
 * layout updates are applied once it shows (or dropped if it is closed before showing). The app's
 * own windows, and small windows attached to a dialog (menus, spinners), are never delayed.
 */
internal class ExtensionPopupQueue(private val delegate: WindowManager) : WindowManager by delegate {
    private var showing: View? = null
    private val pending = ArrayDeque<Pair<View, ViewGroup.LayoutParams>>()

    override fun addView(view: View, params: ViewGroup.LayoutParams) {
        if (!isTopLevelWindow(params) || !openedByExtension()) {
            delegate.addView(view, params)
            return
        }
        if (showing != null) {
            pending.addLast(view to params)
            return
        }
        showing = view
        delegate.addView(view, params)
    }

    override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
        val index = pending.indexOfFirst { it.first === view }
        if (index >= 0) pending[index] = view to params else delegate.updateViewLayout(view, params)
    }

    override fun removeView(view: View) {
        if (dropPending(view)) return
        delegate.removeView(view)
        showNextAfter(view)
    }

    override fun removeViewImmediate(view: View) {
        if (dropPending(view)) return
        delegate.removeViewImmediate(view)
        showNextAfter(view)
    }

    private fun dropPending(view: View): Boolean = pending.removeAll { it.first === view }

    private fun showNextAfter(view: View) {
        if (showing !== view) return
        showing = null
        while (pending.isNotEmpty()) {
            val (next, params) = pending.removeFirst()
            val shown = runCatching { delegate.addView(next, params) }.isSuccess
            if (shown) {
                showing = next
                return
            }
        }
    }

    private fun isTopLevelWindow(params: ViewGroup.LayoutParams): Boolean {
        val type = (params as? WindowManager.LayoutParams)?.type ?: return false
        return type in WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW..WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
    }

    /** True when code from an installed extension (not the app or a library it ships) opens the window. */
    private fun openedByExtension(): Boolean =
        Thread.currentThread().stackTrace.any { frame -> HostPrefixes.none(frame.className::startsWith) }

    private companion object {
        val HostPrefixes = listOf(
            "android.", "androidx.", "com.android.", "java.", "javax.", "jdk.", "sun.", "dalvik.",
            "libcore.", "kotlin.", "kotlinx.", "com.nuvio.", "com.lagradost.", "com.google.",
            "org.jetbrains.", "okhttp3.", "okio.", "coil3.", "io.ktor.", "io.sentry.", "org.chromium.",
            "com.fasterxml.", "org.jsoup.", "app.cash.", "com.kyant.", "Java_", "<",
        )
    }
}
