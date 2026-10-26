package gov.anzong.androidnga.ui.miuix

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

    /**
     * Miuix icons and colours for one floor's header and action row. Called once per view
     * holder; view ids come from fragment_article_list_item.
     */
    @JvmStatic
    fun styleFloorViews(item: View) {
        val context = item.context
        val summary = ContextCompat.getColor(context, R.color.miuix_on_surface_summary)
        val tint = ColorStateList.valueOf(summary)
        // The classic layout gives thumb up/down a 24dp box with 3dp padding (18dp glyph) but
        // lets reply and more wrap their 24dp drawables, so they looked a size larger. All four
        // Miuix actions share one box and inset, so the glyphs render at the same 18dp.
        val box = (24 * item.resources.displayMetrics.density).toInt()
        val inset = (3 * item.resources.displayMetrics.density).toInt()
        listOf(
            R.id.iv_support to R.drawable.ic_miuix_like,
            R.id.iv_oppose to R.drawable.ic_miuix_dislike,
            R.id.iv_reply to R.drawable.ic_miuix_reply,
            R.id.iv_more to R.drawable.ic_miuix_more,
        ).forEach { (id, icon) ->
            item.findViewById<ImageView>(id)?.apply {
                setImageResource(icon)
                imageTintList = tint
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(inset, inset, inset, inset)
                layoutParams = layoutParams.apply {
                    width = box
                    height = box
                }
            }
        }
        // Classic beige metadata (floor, time, posts/location) becomes the Miuix summary colour.
        listOf(R.id.tv_floor, R.id.tv_post_time, R.id.tv_detail, R.id.tv_score).forEach { id ->
            item.findViewById<TextView>(id)?.setTextColor(summary)
        }
        item.findViewById<TextView>(R.id.tv_nickName)?.setTextColor(
            ContextCompat.getColor(context, R.color.miuix_primary),
        )
    }

    /** Styles a floor card once, when its view holder is created. */
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
