package com.lagradost.cloudstream3

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * Current-activity ABI used by extensions that need a UI-bound Android context.
 *
 * Extensions use it to draw their own windows on the app's screen: donation and Discord popups at
 * start-up as much as real settings. The activity is only handed out while the user opens an
 * extension's settings or while an extension searches, loads or resolves links ([withUi]); the rest
 * of the time (their start-up, the home catalogs) extensions get null, so their popups never appear. Some extensions
 * need the activity to search at all.
 */
object CommonActivity {
    private var activityReference: WeakReference<Activity>? = null

    /** Number of operations that currently allow extensions to reach the screen. */
    private val uiRequests = AtomicInteger(0)

    var activity: Activity?
        get() = if (uiRequests.get() > 0) activityReference?.get() else null
        private set(value) {
            activityReference = value?.let(::WeakReference)
        }

    fun setActivityInstance(newActivity: Activity?) {
        activity = newActivity
    }

    /** Lets extensions reach the screen while [block] runs (settings, search, load, links). */
    inline fun <T> withUi(block: () -> T): T {
        allowUi(true)
        try {
            return block()
        } finally {
            allowUi(false)
        }
    }

    @PublishedApi
    internal fun allowUi(allowed: Boolean) {
        if (allowed) {
            uiRequests.incrementAndGet()
            // Extension popups that open now are shown one at a time.
            com.nuvio.app.features.cloudstream.ExtensionPopupQueue.watch()
        } else uiRequests.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
    }
}
