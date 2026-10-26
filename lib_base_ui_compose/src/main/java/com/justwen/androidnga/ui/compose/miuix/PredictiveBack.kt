package com.justwen.androidnga.ui.compose.miuix

import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.PreferenceUtils
import gov.anzong.androidnga.common.PreferenceKey

/**
 * Settings › 外观设置 › 预测返回手势 (off by default).
 *
 * With target SDK 36 the system runs predictive back for every activity: during an edge swipe it
 * drives the cross-activity animation itself from the gesture progress. On launchers whose
 * full-screen gesture does not feed that progress continuously (HyperOS on the Xiaomi 13 Pro),
 * the close animation then plays in pieces and jumps. When the setting is off, every activity
 * registers [PageBackCallback] at the bottom of its dispatcher. An enabled callback makes the
 * system treat back as app-handled, so it does not start the predictive animation; on release
 * the callback finishes the activity, which plays the regular (Miuix or classic) close
 * transition once, completely. Callbacks registered later (pages, panels, drawers) still run
 * first, so their back handling is unchanged.
 *
 * The official per-activity opt-out, android:enableOnBackInvokedCallback, is fixed in the
 * manifest; a callback keeps the setting switchable at runtime.
 */
object PredictiveBack {

    const val KEY = PreferenceKey.KEY_PREDICTIVE_BACK
    const val DEFAULT = false

    /** Read from the app's default preferences, where the settings switch stores it. */
    @JvmStatic
    fun isEnabled(): Boolean = PreferenceUtils.getData(KEY, DEFAULT)

    /**
     * Call from onCreate before any other back callback is added, so the page callback sits at
     * the bottom of the chain. The callback follows the setting when it changes and on every
     * resume, so turning it on or off applies to pages that are already open.
     */
    @JvmStatic
    fun install(activity: ComponentActivity) {
        val callback = PageBackCallback(
            enabledInSettings = { isEnabled() },
            // The root page leaves the task like the system default back (activity kept), so
            // returning to the app does not cold start; inner pages finish.
            finish = { if (activity.isTaskRoot) activity.moveTaskToBack(false) else activity.finish() },
        )
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        activity.lifecycle.addObserver(callback.resumeObserver)
        // Apply a change at once, also to the settings page the switch lives on. SharedPreferences
        // keeps listeners weakly; the callback (held by the dispatcher) keeps this one alive.
        val prefs = ContextUtils.getDefaultSharedPreferences()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) callback.refresh()
        }
        callback.preferenceListener = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
                callback.preferenceListener = null
            }
        })
    }
}

/**
 * Lowest-priority back callback: enabled exactly while predictive back is off in settings.
 * Kept free of Android types except [OnBackPressedCallback] so it can be unit tested.
 */
class PageBackCallback(
    private val enabledInSettings: () -> Boolean,
    private val finish: () -> Unit,
) : OnBackPressedCallback(!enabledInSettings()) {

    val resumeObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) = refresh()
    }

    /** Strong reference for the registered preference listener (see [PredictiveBack.install]). */
    internal var preferenceListener: Any? = null

    /** Follow the setting; called on resume. */
    fun refresh() {
        isEnabled = !enabledInSettings()
    }

    override fun handleOnBackPressed() {
        finish()
    }
}
