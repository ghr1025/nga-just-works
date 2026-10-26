package gov.anzong.androidnga.base.util;

import android.graphics.Color;
import android.widget.Toast;

import androidx.annotation.StringRes;

import es.dmoral.toasty.Toasty;

/**
 * Created by Justwen on 2018/8/11.
 */
public class ToastUtils {

    /**
     * Replaces the classic Toasty toasts while it returns true (the Miuix appearance). Set by the
     * application, since this module sits below the UI modules that know the appearance.
     */
    public interface Override {
        /** @return true if the message was shown, false to fall back to the classic toast */
        boolean show(String text);
    }

    private static volatile Override sOverride;

    public static void setOverride(Override override) {
        sOverride = override;
    }

    /** Shows the message with the override on the main thread; false if there is none. */
    private static boolean overridden(String text) {
        Override override = sOverride;
        if (override == null) {
            return false;
        }
        if (!ThreadUtils.isMainThread()) {
            ThreadUtils.runOnMainThread(() -> {
                if (!override.show(text)) {
                    Toast.makeText(ContextUtils.getContext(), text, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        return override.show(text);
    }

    @Deprecated
    public static void showShortToast(String text) {
        info(text);
    }

    @Deprecated
    public static void showShortToast(@StringRes int strId) {
        showShortToast(ContextUtils.getString(strId));
    }

    @Deprecated
    public static void showToast(String text) {
        showShortToast(text);
    }

    public static void show(String text) {
        if (overridden(text)) {
            return;
        }
        ThreadUtils.runOnMainThread(() -> Toast.makeText(ContextUtils.getContext(), text, Toast.LENGTH_SHORT).show());
    }

    @Deprecated
    public static void showToast(@StringRes int strId) {
        showShortToast(strId);
    }

    public static void success(@StringRes int id) {
        success(ContextUtils.getString(id));
    }

    public static void error(@StringRes int id) {
        error(ContextUtils.getString(id));
    }

    public static void info(@StringRes int id) {
        info(ContextUtils.getString(id));
    }

    public static void success(String text) {
        if (overridden(text)) {
            return;
        }
        if (!ThreadUtils.isMainThread()) {
            ThreadUtils.runOnMainThread(() -> ToastUtils.success(text));
        } else {
            Toasty.custom(ContextUtils.getContext(), text, ContextUtils.getDrawable(es.dmoral.toasty.R.drawable.ic_check_white_24dp),
                    ThemeUtils.getAccentColor(), Color.WHITE, Toast.LENGTH_SHORT, true, true)
                    .show();
        }
    }

    public static void error(String text) {
        if (overridden(text)) {
            return;
        }
        if (!ThreadUtils.isMainThread()) {
            ThreadUtils.runOnMainThread(() -> Toasty.error(ContextUtils.getContext(), text).show());
        } else {
            Toasty.error(ContextUtils.getContext(), text).show();
        }
    }

    public static void info(String text) {
        if (overridden(text)) {
            return;
        }
        if (!ThreadUtils.isMainThread()) {
            ThreadUtils.runOnMainThread(() -> ToastUtils.info(text));
        } else {
            Toasty.custom(ContextUtils.getContext(), text, null, ThemeUtils.getAccentColor(), Color.WHITE, Toast.LENGTH_SHORT, false, false)
                    .show();
        }
    }

    public static void warn(String text) {
        if (overridden(text)) {
            return;
        }
        Toasty.warning(ContextUtils.getContext(), text).show();
    }

}
