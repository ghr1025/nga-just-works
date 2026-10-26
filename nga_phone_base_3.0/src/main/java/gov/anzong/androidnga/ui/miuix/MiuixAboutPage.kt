package gov.anzong.androidnga.ui.miuix

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.justwen.androidnga.ui.compose.miuix.MiuixGroupCard
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPage
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.SmallTitle

/**
 * Renders the About screen's MaterialAboutList with Miuix cards. The list (texts, click and
 * long-click actions) still comes from AboutActivity, so both appearances stay in sync.
 */
object MiuixAboutPage {

    @JvmStatic
    fun show(activity: ComponentActivity, title: CharSequence, list: MaterialAboutList) {
        activity.setContent {
            NgaMiuixTheme {
                AboutPage(activity, title.toString(), list)
            }
        }
    }
}

@Composable
private fun AboutPage(activity: Activity, title: String, list: MaterialAboutList) {
    MiuixPage(
        title = title,
        navIcon = MiuixNavIcon.Back,
        onNavigationClick = {
            (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: activity.finish()
        },
    ) { page ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(page.scrollModifier),
            contentPadding = miuixListPadding(page.padding),
        ) {
            item(key = "gap") { Box(Modifier.padding(top = 12.dp)) }
            items(list.cards, key = { it.id }) { card ->
                val cardTitle = card.title?.toString()
                    ?: card.titleRes.takeIf { it != 0 }?.let(activity::getString)
                if (!cardTitle.isNullOrEmpty()) {
                    SmallTitle(text = cardTitle)
                }
                MiuixGroupCard {
                    card.items.forEach { item ->
                        when (item) {
                            is MaterialAboutActionItem -> AboutActionRow(activity, item)
                            is MaterialAboutTitleItem -> BasicComponent(
                                title = item.text?.toString() ?: activity.getString(item.textRes),
                                summary = item.desc?.toString(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutActionRow(activity: Activity, item: MaterialAboutActionItem) {
    val text = item.text?.toString() ?: item.textRes.takeIf { it != 0 }?.let(activity::getString).orEmpty()
    val sub = item.subText?.toString() ?: item.subTextRes.takeIf { it != 0 }?.let(activity::getString)
    val click = item.onClickAction
    val longClick = item.onLongClickAction
    BasicComponent(
        title = text,
        summary = sub,
        modifier = if (click != null || longClick != null) {
            Modifier.combinedClickable(
                onClick = { click?.onClick() },
                onLongClick = longClick?.let { action -> { action.onClick() } },
            )
        } else {
            Modifier
        },
    )
}
