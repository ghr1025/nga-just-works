package gov.anzong.androidnga.ui.miuix

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import gov.anzong.androidnga.R

/**
 * Miuix styling of the classic profile page (activity_user_profile): the cream cards become
 * Miuix cards, labels and values take Miuix text colours, section titles drop the
 * "::" decoration and use the Miuix small-title look, and the header is a soft Miuix surface
 * instead of the saturated classic banner. Views, bindings and loading stay classic.
 */
object MiuixProfileStyle {

    @JvmStatic
    fun style(root: View) {
        val context = root.context
        val density = root.resources.displayMetrics.density
        val surface = ContextCompat.getColor(context, R.color.miuix_surface)
        val card = ContextCompat.getColor(context, R.color.miuix_surface_container)
        val primary = ContextCompat.getColor(context, R.color.miuix_primary)
        val summary = ContextCompat.getColor(context, R.color.miuix_on_surface_summary)
        val onSurface = ContextCompat.getColor(context, R.color.miuix_on_surface)
        root.setBackgroundColor(surface)

        // Header: avatar and uid on the Miuix surface; the blurred avatar backdrop only shows
        // faintly, and the uid uses the regular text colour instead of white on blue.
        root.findViewById<View>(R.id.toolbar_layout)?.setBackgroundColor(surface)
        root.findViewById<View>(R.id.app_bar)?.setBackgroundColor(surface)
        root.findViewById<View>(R.id.iv_toolbar_layout_bg)?.alpha = 0.12f
        root.findViewById<TextView>(R.id.tv_uid)?.setTextColor(onSurface)

        val radius = 16 * density
        forEachDescendant(root) { view ->
            when {
                view is CardView -> {
                    view.setCardBackgroundColor(card)
                    view.radius = radius
                    view.cardElevation = 0f
                    view.maxCardElevation = 0f
                    (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                        it.leftMargin = (12 * density).toInt()
                        it.rightMargin = (12 * density).toInt()
                    }
                }
                view is TextView -> {
                    styleText(view, primary, summary, onSurface)
                    if (view.id == R.id.tv_user_email) {
                        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = 0
                    }
                }
            }
        }
        // The modify-signature action reads as a Miuix link.
        root.findViewById<TextView>(R.id.btn_modify_sign)?.apply {
            setTextColor(primary)
            background = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(ColorUtils.setAlphaComponent(primary, 0x14))
            }
        }
    }

    private fun styleText(view: TextView, primary: Int, summary: Int, onSurface: Int) {
        val text = view.text?.toString().orEmpty()
        when {
            // ":: 基础信息 ::" -> "基础信息" in the Miuix section-title style
            text.startsWith("::") && text.endsWith("::") -> {
                view.text = text.removePrefix("::").removeSuffix("::").trim()
                view.setTextColor(summary)
                view.paint.isFakeBoldText = false
                view.textSize = 14f
            }
            view.id == R.id.tv_admin_sub_title || view.id == R.id.tv_fame_sub_title -> {
                // Section descriptions read as Miuix summaries, a step below body text.
                view.setTextColor(summary)
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            view.id == R.id.tv_user_state -> Unit // keeps the active / muted / nuked colour
            view.id == R.id.btn_modify_sign -> Unit
            view.currentTextColor == Color.WHITE -> view.setTextColor(onSurface)
            isLabel(view) -> {
                view.setTextColor(onSurface)
                // Labels mix "属地：" and "邮箱 : "; use one form.
                view.text = text.trimEnd().removeSuffix("：").removeSuffix(":").trimEnd() + " : "
            }
            else -> view.setTextColor(summary)
        }
        if (view.id == R.id.tv_user_state && text == "已激活") view.setTextColor(primary)
    }

    /** Label views are the "xxx : " captions next to values. */
    private fun isLabel(view: TextView): Boolean {
        val name = runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull() ?: return false
        return name.endsWith("_title") && !name.startsWith("tv_base_info") && !name.startsWith("tv_sign") &&
            !name.startsWith("tv_admin_title") && !name.startsWith("tv_fame_title")
    }

    private fun forEachDescendant(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) forEachDescendant(view.getChildAt(i), action)
        }
    }

    /** Background and text colours for the profile's HTML sections (sign, admin, fame). */
    @JvmStatic
    fun webColors(view: View): IntArray {
        val context = view.context
        return intArrayOf(
            ContextCompat.getColor(context, R.color.miuix_surface_container),
            ContextCompat.getColor(context, R.color.miuix_on_surface),
        )
    }
}
