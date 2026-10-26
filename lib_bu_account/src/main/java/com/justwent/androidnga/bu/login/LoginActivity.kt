package com.justwent.androidnga.bu.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Route
import com.justwen.androidnga.base.activity.ARouterConstants
import com.justwen.androidnga.ui.compose.BaseComposeActivity
import com.justwen.androidnga.ui.compose.miuix.MiuixPageBox
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.theme.miuixShape
import gov.anzong.androidnga.base.util.ToastUtils

@Route(path = ARouterConstants.ACTIVITY_LOGIN)
class LoginActivity : BaseComposeActivity() {

    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(this)[LoginViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isMiuixStyle) {
            // The Miuix page shows this as a notice card above the login form instead.
            ToastUtils.info("不支持QQ和微博登录");
        }
    }

    /**
     * NGA's login form is its own web page (captcha, third-party flows), so the Miuix page frames
     * it: Miuix top app bar, a notice card instead of the toast, and a loading line while the
     * page loads. The WebView and the login callback are the classic ones.
     */
    @Composable
    override fun MiuixContentView(page: MiuixPageContent) {
        var progress by remember { mutableIntStateOf(0) }
        MiuixPageBox(page.padding) {
            Column(Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    BasicComponent(
                        title = "使用 NGA 账号登录",
                        summary = "不支持 QQ 和微博登录",
                    )
                }
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(miuixShape(16.dp)),
                ) {
                    LoginWebView(
                        url = LoginViewModel.URL_LOGIN,
                        onLoginCallback = {
                            if (viewModel.checkLoginResult(it)) {
                                setResult(RESULT_OK)
                            }
                        },
                        onProgress = { progress = it },
                    )
                }
            }
        }
    }

    @Composable
    override fun ContentView() {
        LoginWebView(url = LoginViewModel.URL_LOGIN, onLoginCallback = {
            if (viewModel.checkLoginResult(it)) {
                setResult(RESULT_OK)
            }
        })
    }

    override fun finish() {
        super.finish()
        if (viewModel.checkLoginResult()) {
            setResult(RESULT_OK)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun LoginWebView(
        url: String,
        onLoginCallback: ValueCallback<Pair<String, String>>,
        onProgress: (Int) -> Unit = {},
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webChromeClient = object : WebChromeClient() {

                        override fun onJsConfirm(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            onLoginCallback.onReceiveValue(Pair(url!!, message!!))
                            return super.onJsConfirm(view, url, message, result)
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            onProgress(newProgress)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (isMiuixStyle) {
                                // NGA's login page paints its body grey around a narrower cream
                                // column; use the cream behind it so the form fills the area.
                                view?.evaluateJavascript(MIUIX_LOGIN_PAGE_JS, null)
                            }
                        }


                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.url?.let {
                                viewModel.currentUrl = it.toString()
                                view?.loadUrl(it.toString())
                            }
                            return true
                        }
                    }
                    getSettings().apply {
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        // NGA's login page is narrower than the screen; fit it to the width
                        // instead of showing the WebView background beside it.
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    // NGA login page background, so any remaining margin blends in.
                    setBackgroundColor(0xFFFFF8E7.toInt())
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }, update = {
                it.loadUrl(url)
            })
    }

}

/** Replaces the login page's grey page background with its cream content colour. */
private const val MIUIX_LOGIN_PAGE_JS = """
(function () {
  var cream = '#FFF6DF';
  document.documentElement.style.background = cream;
  document.body.style.background = cream;
})();
"""
