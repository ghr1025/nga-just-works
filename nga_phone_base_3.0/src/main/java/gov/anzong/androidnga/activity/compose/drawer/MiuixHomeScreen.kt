package gov.anzong.androidnga.activity.compose.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.justwen.androidnga.ui.compose.miuix.MiuixActions
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixNavigationIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPopupMenu
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
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixShape

/** Entry of the home drawer; the click goes to the existing NavigationDrawerViewModel action. */
internal class MiuixDrawerEntry(
    val title: String,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: () -> Unit,
) {
    fun copyWithClick(click: () -> Unit) = MiuixDrawerEntry(title, icon, badge, click)
}

/**
 * Miuix home: the board pager under a small Miuix top app bar, inside the existing
 * [HomeNavigationDrawer] gesture shell, with a HyperOS card-style drawer sheet.
 */
@Composable
internal fun MiuixHomeScreen(
    title: String,
    menu: List<OptionMenuData>,
    forumBoardViewModel: ForumBoardViewModel,
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
                // Close the drawer before an entry runs, so pages open from the home screen and
                // dialogs are not dimmed twice by the drawer scrim.
                fun closingThen(action: () -> Unit): () -> Unit = {
                    scope.launch { drawerState.close() }
                    action()
                }
                MiuixDrawerSheet(
                    entries = entries.map { it.copyWithClick(closingThen(it.onClick)) },
                    footer = footer.copyWithClick(closingThen(footer.onClick)),
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
    onAccountClick: (User?, Int) -> Unit,
) {
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
                entries.forEach { entry -> DrawerRow(entry, entry.badge) }
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
                imageVector = entry.icon,
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
                // A logged-in account without an uploaded avatar shows NGA's default avatar, the
                // same image the profile page and floors use; the grey silhouette stays for
                // the logged-out state.
                val fallback = if (user != null) {
                    painterResource(com.justwen.androidnga.module.message.R.drawable.default_avatar)
                } else {
                    painterResource(R.drawable.drawerdefaulticon)
                }
                Image(
                    painter = if (avatar.isNullOrEmpty()) {
                        fallback
                    } else {
                        rememberAsyncImagePainter(model = avatar, placeholder = fallback, error = fallback)
                    },
                    colorFilter = if (user == null) ColorFilter.tint(colors.onSurfaceVariantSummary) else null,
                    contentDescription = null,
                    // Fill the frame and clip to a true circle: with the default Fit scaling a
                    // non-square avatar left the rest of the clip empty and read as a square.
                    contentScale = ContentScale.Crop,
                    // requiredSize: BasicComponent shares the row by weight when the account text
                    // is long (several accounts), which squeezed size(52.dp) into an ellipse.
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .requiredSize(52.dp)
                        .clip(CircleShape),
                )
            },
            endActions = {
                if (count > 1) {
                    AccountSwitcher(users = users, activeIndex = (activeIndex ?: 0).coerceAtLeast(0) % count)
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
                text = if (user != null) "UID ${user.userId}" else "点击登录账号",
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Switch button of the account card with several accounts: a Miuix "switch" icon with the number of
 * accounts as a small badge; it opens the Miuix list popup of the accounts, the current one checked,
 * opening downwards from the button like the top bar's overflow menu.
 */
@Composable
private fun AccountSwitcher(users: List<User>, activeIndex: Int) {
    val colors = MiuixTheme.colorScheme
    var showPopup by remember { mutableStateOf(false) }
    var holdDown by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = {
                showPopup = true
                holdDown = true
            },
            holdDownState = holdDown,
        ) {
            Icon(
                imageVector = MiuixIcons.Replace,
                contentDescription = "切换账号，共${users.size}个",
                tint = colors.onSurfaceVariantActions,
            )
        }
        // Account count badge on the button's top-end corner.
        Text(
            text = users.size.toString(),
            color = colors.onPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .defaultMinSize(minWidth = 16.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        WindowListPopup(
            show = showPopup,
            // Right edge on the button's right edge, just below it, never past the side margin
            // (the stock context-menu provider insets the right edge by 20dp and clamps the left
            // edge to the bare screen edge).
            popupPositionProvider = MiuixPopupMenu.BelowEndPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showPopup = false },
            onDismissFinished = { holdDown = false },
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                users.forEachIndexed { index, account ->
                    key(account.userId) {
                        DropdownImpl(
                            // DropdownImpl wraps text up to 200dp and the list grows to fit; a capped
                            // one-line name keeps the popup narrow enough to stay aligned with the
                            // switch button instead of being pushed to the side margin.
                            text = accountLabel(account),
                            optionSize = users.size,
                            isSelected = index == activeIndex,
                            index = index,
                            onSelectedIndexChange = { selected ->
                                dismiss?.invoke()
                                if (selected != activeIndex) UserManager.setActiveIndex(selected)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Account name for the switch popup: nickname (or UID), cut to [MAX_LABEL_WIDTH] display columns
 * (CJK characters count two, Latin one) so a long name stays on one line.
 */
internal fun accountLabel(account: User): String {
    val name = account.nickName?.takeIf { it.isNotEmpty() } ?: "UID ${account.userId}"
    return ellipsizeColumns(name, MAX_LABEL_WIDTH)
}

internal fun ellipsizeColumns(text: String, maxColumns: Int): String {
    fun columns(c: Char) = if (c.code >= 0x2E80) 2 else 1
    if (text.sumOf(::columns) <= maxColumns) return text
    val out = StringBuilder()
    var used = 0
    for (c in text) {
        if (used + columns(c) > maxColumns - 1) break
        out.append(c)
        used += columns(c)
    }
    return out.append('…').toString()
}

private const val MAX_LABEL_WIDTH = 20
