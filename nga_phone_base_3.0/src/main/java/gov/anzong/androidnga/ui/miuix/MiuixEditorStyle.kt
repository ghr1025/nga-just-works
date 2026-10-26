package gov.anzong.androidnga.ui.miuix

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import android.widget.TextView
import android.view.ViewStub
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
        // The emoticon, formatting and category panels are ViewStubs inflated on first use;
        // style each one when it appears.
        listOf(R.id.bottom_emoticon_stub, R.id.bottom_text_stub, R.id.bottom_category_stub).forEach { id ->
            root.findViewById<ViewStub>(id)?.setOnInflateListener { _, inflated -> stylePanel(inflated) }
        }
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

    /** Miuix colours for an editor panel: surface background, Miuix text, primary accents. */
    @JvmStatic
    fun stylePanel(panel: View) {
        val context = panel.context
        val density = panel.resources.displayMetrics.density
        val surface = ContextCompat.getColor(context, R.color.miuix_surface)
        val card = ContextCompat.getColor(context, R.color.miuix_surface_container)
        val onSurface = ContextCompat.getColor(context, R.color.miuix_on_surface)
        val summary = ContextCompat.getColor(context, R.color.miuix_on_surface_summary)
        val primary = ContextCompat.getColor(context, R.color.miuix_primary)
        panel.setBackgroundColor(surface)
        forEach(panel) { view ->
            when (view) {
                is TabLayout -> {
                    // Miuix TabRow look: rounded track, the selected tab as a rounded block,
                    // bold dark selected text, grey others; no underline.
                    view.background = GradientDrawable().apply {
                        cornerRadius = 12 * density
                        setColor(androidx.core.graphics.ColorUtils.blendARGB(surface, onSurface, 0.04f))
                    }
                    view.setSelectedTabIndicator(GradientDrawable().apply {
                        cornerRadius = 12 * density
                        setColor(card)
                    })
                    view.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_STRETCH)
                    view.setSelectedTabIndicatorColor(card)
                    view.setTabTextColors(summary, onSurface)
                    view.tabRippleColor = null
                    view.isTabIndicatorFullWidth = true
                    (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
                        (12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (6 * density).toInt(),
                    )
                    view.setPadding((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
                    view.clipToPadding = true
                }
                is android.widget.Spinner -> {
                    view.background = GradientDrawable().apply {
                        cornerRadius = 12 * density
                        setColor(card)
                    }
                }
                is TextView -> {
                    // Formatting buttons: rounded Miuix chips.
                    if (view.isClickable && view.parent is android.widget.TableRow) {
                        view.background = GradientDrawable().apply {
                            cornerRadius = 12 * density
                            setColor(card)
                        }
                        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
                            (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(),
                        )
                        view.setTextColor(onSurface)
                    } else if (view.currentTextColor != primary) {
                        view.setTextColor(onSurface)
                    }
                }
                is android.widget.ListView -> {
                    view.setBackgroundColor(card)
                    view.divider = android.graphics.drawable.ColorDrawable(
                        ContextCompat.getColor(context, R.color.miuix_divider_line),
                    )
                    view.dividerHeight = 1
                }
                is android.widget.ProgressBar -> view.indeterminateTintList = ColorStateList.valueOf(primary)
            }
        }
    }

    private fun forEach(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) forEach(view.getChildAt(i), action)
    }

    /**
     * Miuix form styling for the simple classic editors (signature, avatar URL): rounded Miuix
     * input fields, Miuix text colours, and primary/secondary Miuix buttons.
     */
    @JvmStatic
    fun styleForm(root: View) {
        val context = root.context
        val density = root.resources.displayMetrics.density
        val surface = ContextCompat.getColor(context, R.color.miuix_surface)
        val field = ContextCompat.getColor(context, R.color.miuix_surface_container)
        // Miuix grey for secondary buttons (secondaryVariant), as the dialog cancel button.
        val secondary = ContextCompat.getColor(context, R.color.miuix_dialog_button_secondary)
        val onSurface = ContextCompat.getColor(context, R.color.miuix_on_surface)
        val summary = ContextCompat.getColor(context, R.color.miuix_on_surface_summary)
        val primary = ContextCompat.getColor(context, R.color.miuix_primary)
        root.setBackgroundColor(surface)
        val pad = (14 * density).toInt()
        val margin = (12 * density).toInt()
        var firstButton = true
        forEach(root) { view ->
            when (view) {
                is EditText -> {
                    view.background = GradientDrawable().apply {
                        cornerRadius = 16 * density
                        setColor(field)
                    }
                    view.setPadding(pad, pad, pad, pad)
                    view.setTextColor(onSurface)
                    view.setHintTextColor(summary)
                    (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(margin, margin, margin, margin)
                }
                is android.widget.Button -> {
                    // First button is the secondary action (select picture), the next the primary.
                    val bg = if (firstButton) secondary else primary
                    view.background = GradientDrawable().apply {
                        cornerRadius = 16 * density
                        setColor(bg)
                    }
                    view.setTextColor(if (firstButton) onSurface else ContextCompat.getColor(context, android.R.color.white))
                    view.isAllCaps = false
                    view.stateListAnimator = null
                    firstButton = false
                }
                is TextView -> view.setTextColor(summary)
            }
        }
    }
}
