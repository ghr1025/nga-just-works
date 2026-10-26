package com.justwen.androidnga.ui.compose.miuix

import com.alibaba.android.arouter.launcher.ARouter
import com.justwen.androidnga.base.service.api.IThemeManagerService

/**
 * Appearance switch shared by every module. The application owns the stored value through
 * [IThemeManagerService]; screens only read it once per Activity because changing the
 * appearance recreates the visible Activities.
 */
object NgaUiStyle {

    private fun service(): IThemeManagerService? = runCatching {
        ARouter.getInstance().build(IThemeManagerService.ROUTER_PATH).navigation() as? IThemeManagerService
    }.getOrNull()

    /** Whether pages render with Miuix (HyperOS) components instead of the classic UI. */
    @JvmStatic
    fun isMiuix(): Boolean = service()?.isMiuixStyle() == true

    @JvmStatic
    fun isNightMode(): Boolean = service()?.isNightMode() == true
}
