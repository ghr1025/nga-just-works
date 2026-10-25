package gov.anzong.androidnga.ui.miuix

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import gov.anzong.androidnga.R

/**
 * Miuix styling of the classic floor list: each floor becomes a rounded HyperOS card on the
 * Miuix surface, the refresh spinner takes the Miuix primary colour, and the list clears the
 * navigation bar because Miuix pages draw edge to edge.
 */
object MiuixReaderStyle {

    private const val CARD_RADIUS_DP = 16f
    private const val CARD_MARGIN_H_DP = 12f
    private const val CARD_MARGIN_V_DP = 6f

    @JvmStatic
    fun styleFloorCard(item: View) {
        val density = item.resources.displayMetrics.density
        item.background = GradientDrawable().apply {
            cornerRadius = CARD_RADIUS_DP * density
            setColor(ContextCompat.getColor(item.context, R.color.miuix_surface_container))
        }
        (item.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
            (CARD_MARGIN_H_DP * density).toInt(),
            (CARD_MARGIN_V_DP * density).toInt(),
            (CARD_MARGIN_H_DP * density).toInt(),
            (CARD_MARGIN_V_DP * density).toInt(),
        )
    }

    @JvmStatic
    fun styleFloorList(list: RecyclerView, refresh: SwipeRefreshLayout?, loading: View?) {
        val context = list.context
        val density = list.resources.displayMetrics.density
        list.setBackgroundColor(Color.TRANSPARENT)
        list.updatePadding(top = list.paddingTop + (CARD_MARGIN_V_DP * density).toInt())
        list.clipToPadding = false
        refresh?.setColorSchemeColors(ContextCompat.getColor(context, R.color.miuix_primary))
        refresh?.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context, R.color.miuix_surface_container))
        loading?.setBackgroundColor(ContextCompat.getColor(context, R.color.miuix_surface))
        clearNavigationBar(list)
    }

    /** Adds the navigation bar height to the bottom padding, once insets are known. */
    private fun clearNavigationBar(list: RecyclerView) {
        val basePadding = list.paddingBottom
        fun apply(insets: WindowInsetsCompat?) {
            val bottom = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: return
            list.updatePadding(bottom = basePadding + bottom)
        }
        ViewCompat.setOnApplyWindowInsetsListener(list) { _, insets ->
            apply(insets)
            insets
        }
        // Views hosted inside Compose may not receive dispatched insets; read the root insets.
        list.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = apply(ViewCompat.getRootWindowInsets(v))
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
