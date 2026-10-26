package gov.anzong.androidnga.core;

/** Stylesheet for rendered post bodies, next to the shared style.css. */
public final class HtmlStyleSheets {

    private HtmlStyleSheets() {
    }

    public static String select(boolean darkMode, boolean miuixStyle) {
        if (miuixStyle) {
            return darkMode ? "style_miuix_dark.css" : "style_miuix_light.css";
        }
        return darkMode ? "style_dark.css" : "style_light.css";
    }
}
