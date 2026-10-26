package com.justwen.androidnga.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeState
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.MiuixPageBox
import com.justwen.androidnga.ui.compose.miuix.MiuixScaffoldApp
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.MiuixTransitions
import com.justwen.androidnga.ui.compose.miuix.NgaUiStyle
import com.justwen.androidnga.ui.compose.theme.AppTheme
import com.justwen.androidnga.ui.compose.widget.OptionMenuData
import com.justwen.androidnga.ui.compose.widget.ScaffoldApp
import com.justwen.androidnga.ui.compose.widget.TopAppBarData

abstract class BaseComposeActivity : FragmentActivity(), MiuixChromeOwner {

    override val miuixChrome = MiuixChromeState()

    final override var isMiuixStyle: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        isMiuixStyle = NgaUiStyle.isMiuix()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (isMiuixStyle) {
            MiuixTransitions.apply(this)
        }
        buildContentView()
    }

    override fun onResume() {
        super.onResume()
        // The appearance may have been switched on another screen; rebuild with the new one.
        if (!isFinishing && isMiuixStyle != NgaUiStyle.isMiuix()) {
            recreate()
        }
    }

    fun buildContentView() {
        if (isMiuixStyle) {
            setContent {
                NgaMiuixTheme {
                    MiuixScaffoldApp(
                        topAppBarData = getTopAppBarData(),
                        fabClick = getFabClickAction(),
                    ) { page ->
                        MiuixContentView(page)
                    }
                }
            }
            return
        }
        setContent {
            AppTheme {
                ScaffoldApp(
                    topAppBarData = getTopAppBarData(),
                    fabClick = getFabClickAction(),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        ContentView()
                    }
                }
            }
        }
    }

    open fun getTopAppBarData(): TopAppBarData {
        val topAppBarData = TopAppBarData(title = title.toString())
        topAppBarData.optionMenuData = getOptionMenuData()
        topAppBarData.navigationIconAction = { finish() }
        return topAppBarData
    }

    open fun getOptionMenuData(): List<OptionMenuData>? {
        return null
    }

    open fun getFabClickAction(): (() -> Unit)? {
        return null
    }

    @Composable
    abstract fun ContentView()

    /**
     * Content of the Miuix appearance. Screens override it with Miuix components; the default
     * places the classic content below the Miuix top app bar so a screen is never left without
     * a usable page.
     */
    @Composable
    open fun MiuixContentView(page: MiuixPageContent) {
        MiuixPageBox(page.padding) {
            ContentView()
        }
    }
}