package com.justwen.androidnga.ui.compose.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Foundation >= 1.7 makes the default `Modifier.clickable` require an [IndicationNodeFactory]
 * from [LocalIndication] and throws for legacy indications. Classic pages get their indication
 * from the Material [MaterialTheme], so Material must stay on the same Compose release train as
 * the foundation version Miuix pulls in; otherwise every classic screen crashes on first
 * composition.
 */
class MaterialIndicationCompatTest {

    @Test
    fun materialThemeProvidesTheIndicationFoundationClickableRequires() {
        var indication: Indication? = null
        val scope = CoroutineScope(Dispatchers.Unconfined + BroadcastFrameClock())
        val recomposer = Recomposer(scope.coroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        try {
            composition.setContent {
                MaterialTheme {
                    indication = LocalIndication.current
                }
            }
        } finally {
            composition.dispose()
            recomposer.cancel()
            scope.cancel()
        }
        assertTrue(
            "MaterialTheme must provide an IndicationNodeFactory, got $indication",
            indication is IndicationNodeFactory,
        )
    }

    /** Applier for a composition that emits no nodes; only CompositionLocals are read. */
    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
