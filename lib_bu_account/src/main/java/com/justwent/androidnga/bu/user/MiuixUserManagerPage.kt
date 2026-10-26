package com.justwent.androidnga.bu.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.justwen.androidnga.ui.compose.miuix.MiuixGroupCard
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import com.justwen.androidnga.module.account.R
import com.justwent.androidnga.bu.UserManager
import sp.phone.common.User
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperRadioButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix account page: the active account is a radio choice, each row opens the profile from
 * its avatar area and removes the account from the trailing delete button.
 */
@Composable
internal fun MiuixUserManagerPage(
    page: MiuixPageContent,
    onAddAccount: () -> Unit,
    onShowProfile: (User) -> Unit,
) {
    val users by UserManager.getUserListLiveData().observeAsState(emptyList())
    val activeIndex by UserManager.getActiveIndexLiveData().observeAsState()
    val padding = miuixListPadding(page.padding)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(page.scrollModifier)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.size(padding.calculateTopPadding()))
        if (users.isNotEmpty()) {
            SmallTitle(text = "已登录账号")
            MiuixGroupCard {
                users.forEachIndexed { index, user ->
                    SuperRadioButton(
                        title = user.mNickName.orEmpty(),
                        summary = "UID ${user.userId}",
                        selected = activeIndex == index,
                        onClick = { if (activeIndex != index) UserManager.setActiveIndex(index) },
                        endActions = {
                            UserAvatar(user, onClick = { onShowProfile(user) })
                            IconButton(onClick = { UserManager.removeUser(index) }) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = "删除账号",
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            }
                        },
                    )
                }
            }
        }
        MiuixGroupCard {
            SuperArrow(title = "登录新账号", onClick = onAddAccount)
        }
        Spacer(Modifier.size(padding.calculateBottomPadding()))
    }
}

@Composable
private fun UserAvatar(user: User, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Image(
            painter = rememberAsyncImagePainter(
                model = user.avatarUrl ?: "",
                error = painterResource(id = R.drawable.drawerdefaulticon),
                placeholder = painterResource(id = R.drawable.drawerdefaulticon),
            ),
            contentDescription = "查看资料",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
        )
    }
}
