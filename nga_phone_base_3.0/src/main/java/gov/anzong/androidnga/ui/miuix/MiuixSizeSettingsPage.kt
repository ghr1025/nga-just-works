package gov.anzong.androidnga.ui.miuix

import android.app.Activity
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.MiuixGroupCard
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPage
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One size preference of the font/avatar page: range, current value and how to save it. */
class MiuixSizeItem(
    val title: String,
    val min: Int,
    val max: Int,
    val initial: Int,
    /** Called while dragging, for live previews. */
    val onPreview: (Int) -> Unit = {},
    /** Called when the drag ends; persists like the classic SeekBar's action-up callback. */
    val onCommit: (Int) -> Unit,
)

/**
 * Miuix version of the font and avatar size page: Miuix sliders in cards, the existing
 * PhoneConfiguration setters persist each value when the drag ends, and the classic preview
 * WebView keeps showing the post text at the chosen zoom.
 */
fun miuixSizeSettingsView(activity: Activity, items: List<MiuixSizeItem>, preview: View): View =
    ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NgaMiuixTheme {
                val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
                MiuixPage(
                    title = chrome?.title.orEmpty(),
                    navIcon = MiuixNavIcon.Back,
                    onNavigationClick = {
                        (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: activity.finish()
                    },
                ) { page ->
                    val padding = miuixListPadding(page.padding)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(page.scrollModifier)
                            .verticalScroll(rememberScrollState())
                            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                    ) {
                        SmallTitle(text = "大小")
                        MiuixGroupCard {
                            items.forEach { SizeRow(it) }
                        }
                        SmallTitle(text = "预览")
                        MiuixGroupCard {
                            AndroidView(
                                factory = {
                                    (preview.parent as? android.view.ViewGroup)?.removeView(preview)
                                    preview
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
private fun SizeRow(item: MiuixSizeItem) {
    var value by remember { mutableFloatStateOf(item.initial.coerceIn(item.min, item.max).toFloat()) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.roundToInt().toString(),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Box(Modifier.padding(top = 10.dp)) {
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    item.onPreview(it.roundToInt())
                },
                valueRange = item.min.toFloat()..item.max.toFloat(),
                onValueChangeFinished = { item.onCommit(value.roundToInt()) },
            )
        }
    }
}

/** Applies the post text zoom to the classic preview WebView. */
fun applyPreviewZoom(webView: WebView, zoom: Int) {
    webView.settings.textZoom = zoom
}
