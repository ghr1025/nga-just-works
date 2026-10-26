package gov.anzong.androidnga.ui.miuix

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import gov.anzong.androidnga.R

/**
 * Miuix styling of the classic post editor (fragment_topic_post): rounded Miuix input fields,
 * a neutral Miuix tool bar instead of the classic coloured strip, Miuix checkbox tint, and the
 * tool bar lifted above the navigation bar. Views, listeners and the editor logic stay as they
 * are; only colours, backgrounds and paddings change.
 */
object MiuixEditorStyle {

    @JvmStatic
    fun style(root: View) {
        val context = root.context
        val density = root.resources.displayMetrics.density
        val surface = ContextCompat.getColor(context, R.color.miuix_surface)
        val field = ContextCompat.getColor(context, R.color.miuix_surface_container)
        val summary = ContextCompat.getColor(context, R.color.miuix_on_surface_summary)
        val primary = ContextCompat.getColor(context, R.color.miuix_primary)
        root.setBackgroundColor(surface)

        fun rounded(color: Int) = GradientDrawable().apply {
            cornerRadius = 16 * density
            setColor(color)
        }
        val pad = (14 * density).toInt()
        listOf(R.id.reply_titile_edittext, R.id.reply_body_edittext).forEach { id ->
            root.findViewById<EditText>(id)?.apply {
                background = rounded(field)
                setPadding(pad, pad, pad, pad)
                setHintTextColor(summary)
                (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
                    (12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt(),
                )
            }
        }
        root.findViewById<CheckBox>(R.id.anony)?.buttonTintList = ColorStateList.valueOf(primary)

        // The tool bar strip is the LinearLayout holding the four ImageButtons.
        val bar = root.findViewById<ImageButton>(R.id.btn_category)?.parent as? View ?: return
        bar.setBackgroundColor(surface)
        listOf(R.id.btn_category, R.id.btn_text, R.id.btn_attachment, R.id.btn_emoticon).forEach { id ->
            root.findViewById<ImageButton>(id)?.imageTintList = ColorStateList.valueOf(summary)
        }
        val panel = root.findViewById<View>(R.id.control_panel) ?: return
        val basePadding = panel.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(panel) { view, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = basePadding + nav.bottom)
            insets
        }
        panel.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = ViewCompat.requestApplyInsets(v)
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
