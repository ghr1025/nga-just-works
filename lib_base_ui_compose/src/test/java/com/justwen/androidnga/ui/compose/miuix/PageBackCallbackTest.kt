package com.justwen.androidnga.ui.compose.miuix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageBackCallbackTest {

    @Test
    fun predictiveBackOffKeepsTheCallbackEnabledSoTheSystemAnimationDoesNotRun() {
        val callback = PageBackCallback(enabledInSettings = { false }, finish = {})
        assertTrue(callback.isEnabled)
    }

    @Test
    fun predictiveBackOnLeavesBackToTheSystem() {
        val callback = PageBackCallback(enabledInSettings = { true }, finish = {})
        assertFalse(callback.isEnabled)
    }

    @Test
    fun backFinishesThePageOnce() {
        var finished = 0
        val callback = PageBackCallback(enabledInSettings = { false }, finish = { finished++ })
        callback.handleOnBackPressed()
        assertEquals(1, finished)
    }

    @Test
    fun refreshFollowsAChangedSetting() {
        var predictive = false
        val callback = PageBackCallback(enabledInSettings = { predictive }, finish = {})
        assertTrue(callback.isEnabled)
        predictive = true
        callback.refresh()
        assertFalse(callback.isEnabled)
        predictive = false
        callback.refresh()
        assertTrue(callback.isEnabled)
    }

    @Test
    fun settingIsOffByDefault() {
        assertFalse(PredictiveBack.DEFAULT)
        assertEquals("key_predictive_back", PredictiveBack.KEY)
    }
}
