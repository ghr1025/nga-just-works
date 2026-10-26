package com.justwen.androidnga.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alibaba.android.arouter.launcher.ARouter
import com.justwen.androidnga.base.service.api.IThemeManagerService
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeState
import com.justwen.androidnga.ui.compose.miuix.MiuixTransitions
import com.justwen.androidnga.ui.compose.miuix.PredictiveBack
import com.justwen.androidnga.ui.compose.miuix.NgaUiStyle
import com.justwen.androidnga.ui.compose.miuix.miuixViewShell

abstract class BaseActivity : AppCompatActivity(), MiuixChromeOwner {

    protected var mNaviBarHeight: Int = 0

    final override val miuixChrome = MiuixChromeState()

    final override var isMiuixStyle: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        isMiuixStyle = NgaUiStyle.isMiuix()
        initTheme()
        super.onCreate(savedInstanceState)
        // Before initHandleBackEvent, so that callback keeps running first.
        PredictiveBack.install(this)
        if (isMiuixStyle) {
            MiuixTransitions.apply(this)
        }
        initContentView()
        initHandleBackEvent()
        initEdgeToEdge()
        initStatusBar()
        initNavigationBar()
    }

    private fun initHandleBackEvent() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = onHandleBackEvent()
                if (!isEnabled) {
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    protected fun onHandleBackEvent(): Boolean {
        return false
    }

    private fun initStatusBar() {
        val decorView = window.decorView
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.isAppearanceLightStatusBars = false
    }

    private fun initNavigationBar() {
        val background = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, background, true)
        val backgroundColor = if (background.resourceId != 0) {
            ContextCompat.getColor(this, background.resourceId)
        } else {
            background.data
        }
        window.navigationBarColor = backgroundColor
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.decorView.setBackgroundColor(backgroundColor)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightNavigationBars =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) !=
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun initEdgeToEdge() {
        enableEdgeToEdge()
        val contentView = findViewById<View>(android.R.id.content)
        // Miuix pages pad the system bars inside their own Scaffold.
        if (contentView != null && !isMiuixStyle) {
            val initialPaddingLeft = contentView.paddingLeft
            val initialPaddingTop = contentView.paddingTop
            val initialPaddingRight = contentView.paddingRight
            val initialPaddingBottom = contentView.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(
                contentView
            ) { _, insets ->
                val stateBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val statusView = window.decorView.findViewById<View?>(R.id.status_bar)
                    ?: View(contentView.context).also { created ->
                        created.id = R.id.status_bar
                        val parent = contentView.parent as ViewGroup
                        parent.addView(created, 0)
                    }
                statusView.layoutParams = statusView.layoutParams?.apply {
                    height = stateBars.top
                } ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    stateBars.top
                )
                statusView.setBackgroundColor(getPrimaryColor())

                val navaBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                mNaviBarHeight = navaBars.bottom
                contentView.setPadding(
                    initialPaddingLeft,
                    initialPaddingTop,
                    initialPaddingRight,
                    initialPaddingBottom + navaBars.bottom
                )
                insets
            }
        }
    }

    private fun getPrimaryColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }

    open fun initContentView() {
        if (isMiuixStyle) {
            val classic = layoutInflater.inflate(R.layout.activity_toolbar_template, null)
            setContentView(miuixViewShell(this, classic, classic.findViewById(R.id.main_content)))
            return
        }
        setContentView(R.layout.activity_toolbar_template)
        setupToolbar()
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        miuixChrome.updateTitle(title)
    }

    override fun invalidateOptionsMenu() {
        super.invalidateOptionsMenu()
        miuixChrome.invalidateMenu()
    }

    override fun onResume() {
        super.onResume()
        // The appearance may have been switched on another screen; rebuild with the new one.
        if (!isFinishing && isMiuixStyle != NgaUiStyle.isMiuix()) {
            recreate()
        }
    }

    open fun setupToolbar(toolbar: Toolbar = findViewById(R.id.toolbar)) {
        if (isMiuixStyle) {
            return
        }
        if (supportActionBar == null) {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
            }
        }
    }

    private fun initTheme() {
        val themeManager = ARouter.getInstance().build(IThemeManagerService.ROUTER_PATH)
            .navigation() as IThemeManagerService
        setTheme(themeManager.getTheme())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
