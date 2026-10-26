package com.justwen.androidnga.ui.compose.miuix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Window-level chrome that legacy hosts publish to their Miuix pages: the Activity title (which
 * fragments keep setting through `Activity.setTitle`) and an options-menu generation that bumps
 * whenever the host invalidates its options menu.
 */
class MiuixChromeState {

    var title: String by mutableStateOf("")
        private set

    var menuGeneration: Int by mutableIntStateOf(0)
        private set

    fun updateTitle(title: CharSequence?) {
        this.title = title?.toString().orEmpty()
    }

    fun invalidateMenu() {
        menuGeneration++
    }
}

/** Implemented by Activities whose pages may render with Miuix. */
interface MiuixChromeOwner {
    val miuixChrome: MiuixChromeState

    /** Appearance captured when the Activity was created; stable for the Activity's lifetime. */
    val isMiuixStyle: Boolean
}

/** Whether [activity] renders its pages with Miuix; falls back to the global setting. */
fun isMiuixHost(activity: android.app.Activity?): Boolean =
    (activity as? MiuixChromeOwner)?.isMiuixStyle ?: NgaUiStyle.isMiuix()

/**
 * Marker for fragments that draw a complete Miuix page, including its top app bar, while the
 * Miuix appearance is active. Hosts use it to skip their legacy toolbar and inset handling.
 */
interface MiuixFragment
