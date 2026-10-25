package gov.anzong.androidnga.activity.compose.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.justwen.androidnga.ui.compose.miuix.MiuixActions
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixNavigationIcon
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.toMiuixAction
import com.justwen.androidnga.ui.compose.widget.OptionMenuData
import com.justwent.androidnga.bu.UserManager
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.compose.board.ForumBoardView
import gov.anzong.androidnga.activity.compose.board.ForumBoardViewModel
import kotlinx.coroutines.launch
import sp.phone.common.User
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape

/** Entry of the home drawer; the click goes to the existing NavigationDrawerViewModel action. */
internal class MiuixDrawerEntry(
    val title: String,
    val icon: Int,
    val badge: String? = null,
    val onClick: () -> Unit,
)

/**
 * Miuix home: the board pager under a small Miuix top app bar, inside the existing
 * [HomeNavigationDrawer] gesture shell, with a HyperOS card-style drawer sheet.
 */
@Composable
internal fun MiuixHomeScreen(
    title: String,
    menu: List<OptionMenuData>,
    forumBoardViewModel: ForumBoardViewModel,
    drawerViewModel: NavigationDrawerViewModel,
    entries: List<MiuixDrawerEntry>,
    footer: MiuixDrawerEntry,
    onAccountClick: (User?, Int) -> Unit,
) {
    NgaMiuixTheme {
        val drawerState = rememberHomeDrawerState()
        val scope = rememberCoroutineScope()
        val gestureState = remember { HomeDrawerGestureState() }
        HomeNavigationDrawer(
            drawerState = drawerState,
            gestureState = gestureState,
            drawerContent = {
                MiuixDrawerSheet(
                    entries = entries,
                    footer = footer,
                    replyCountEntryTitle = "最近被喷",
                    drawerViewModel = drawerViewModel,
                    onAccountClick = onAccountClick,
                )
            },
        ) {
            Scaffold(
                topBar = {
                    SmallTopAppBar(
                        title = title,
                        navigationIcon = {
                            MiuixNavigationIcon(MiuixNavIcon.Menu) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        actions = { MiuixActions(menu.map { it.toMiuixAction() }) },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                ) {
                    ForumBoardView(
                        forumBoardViewModel = forumBoardViewModel,
                        pagerModifier = Modifier.onGloballyPositioned {
                            gestureState.pagerBounds = it.boundsInRoot()
                        },
                        onPagerInteractionChanged = {
                            gestureState.settledPage = it?.settledPage ?: -1
                            gestureState.pagerSettled = it?.isScrollInProgress == false
                        },
                        onFavoriteReorderActiveChanged = {
                            gestureState.favoriteReorderActive = it
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixDrawerSheet(
    entries: List<MiuixDrawerEntry>,
    footer: MiuixDrawerEntry,
    replyCountEntryTitle: String,
    drawerViewModel: NavigationDrawerViewModel,
    onAccountClick: (User?, Int) -> Unit,
) {
    val replyCount by drawerViewModel.replyCount.observeAsState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MiuixTheme.colorScheme.surface)
            .padding(top = top + 12.dp, bottom = bottom + 12.dp),
    ) {
        MiuixAccountCard(onAccountClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                entries.forEach { entry ->
                    val badge = if (entry.title == replyCountEntryTitle) {
                        replyCount?.takeIf { it > 0 }?.toString()
                    } else {
                        entry.badge
                    }
                    DrawerRow(entry, badge)
                }
            }
        }
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            DrawerRow(footer, null)
        }
    }
}

@Composable
private fun DrawerRow(entry: MiuixDrawerEntry, badge: String?) {
    val colors = MiuixTheme.colorScheme
    SuperArrow(
        title = entry.title,
        onClick = entry.onClick,
        startAction = {
            Icon(
                painter = painterResource(entry.icon),
                contentDescription = null,
                tint = colors.onSurfaceContainer,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(24.dp),
            )
        },
        endActions = {
            if (badge != null) {
                Text(
                    text = badge,
                    color = colors.onError,
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(miuixShape(10.dp))
                        .background(colors.error)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        },
    )
}

@Composable
private fun MiuixAccountCard(onAccountClick: (User?, Int) -> Unit) {
    val users by UserManager.getUserListLiveData().observeAsState(emptyList())
    val activeIndex by UserManager.getActiveIndexLiveData().observeAsState(0)
    val count = users.size
    val user = users.getOrNull((activeIndex ?: 0).coerceAtLeast(0) % count.coerceAtLeast(1))
    val colors = MiuixTheme.colorScheme
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        BasicComponent(
            onClick = { onAccountClick(user, count) },
            startAction = {
                val avatar = user?.avatarUrl
                Image(
                    painter = if (avatar.isNullOrEmpty()) {
                        painterResource(R.drawable.drawerdefaulticon)
                    } else {
                        rememberAsyncImagePainter(
                            model = avatar,
                            placeholder = painterResource(R.drawable.drawerdefaulticon),
                        )
                    },
                    colorFilter = if (avatar.isNullOrEmpty()) ColorFilter.tint(colors.onSurfaceVariantSummary) else null,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(52.dp)
                        .clip(miuixShape(26.dp)),
                )
            },
            endActions = {
                if (count > 1) {
                    IconButton(onClick = { UserManager.toggleUser(true) }) {
                        Icon(
                            imageVector = MiuixIcons.ChevronForward,
                            contentDescription = "切换账号",
                            tint = colors.onSurfaceVariantActions,
                        )
                    }
                }
            },
        ) {
            Text(
                text = user?.nickName ?: "未登录",
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    count > 1 -> "已登录${count}个账户，点击切换 · UID ${user?.userId}"
                    count == 1 -> "UID ${user?.userId}"
                    else -> "点击登录账号"
                },
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
            )
        }
    }
}
