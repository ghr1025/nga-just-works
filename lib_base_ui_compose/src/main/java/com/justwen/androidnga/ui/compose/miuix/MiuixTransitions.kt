package com.justwen.androidnga.ui.compose.miuix

import android.app.Activity
import android.os.Build

/**
 * Miuix page transitions for Activities: opening slides the new page in from the right with a
 * fade while the page underneath shifts left; going back reverses it, left to right. Uses the
 * Miuix NavDisplay curve (damped spring, 500 ms).
 *
 * overrideActivityTransition (API 34+) also drives the predictive back gesture, so the swipe
 * plays the same close animation as the gesture progresses.
 */
object MiuixTransitions {

    private var openEnter = 0
    private var openExit = 0
    private var closeEnter = 0
    private var closeExit = 0

    /** Called once by the application with its four animation resources. */
    @JvmStatic
    fun register(openEnter: Int, openExit: Int, closeEnter: Int, closeExit: Int) {
        this.openEnter = openEnter
        this.openExit = openExit
        this.closeEnter = closeEnter
        this.closeExit = closeExit
    }

    /** Applies the Miuix open/close transitions to [activity]; call from onCreate. */
    @JvmStatic
    fun apply(activity: Activity) {
        if (openEnter == 0) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, openEnter, openExit)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, closeEnter, closeExit)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(openEnter, openExit)
        }
    }

    /** Pre-34 back transition; on 34+ the CLOSE override from [apply] already covers it. */
    @JvmStatic
    fun applyClose(activity: Activity) {
        if (openEnter == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(closeEnter, closeExit)
    }
}
