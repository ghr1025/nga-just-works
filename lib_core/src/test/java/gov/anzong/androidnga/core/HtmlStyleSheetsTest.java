package gov.anzong.androidnga.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlStyleSheetsTest {

    @Test
    public void classicKeepsItsStylesheets() {
        assertEquals("style_light.css", HtmlStyleSheets.select(false, false));
        assertEquals("style_dark.css", HtmlStyleSheets.select(true, false));
    }

    @Test
    public void miuixUsesItsOwnStylesheets() {
        assertEquals("style_miuix_light.css", HtmlStyleSheets.select(false, true));
        assertEquals("style_miuix_dark.css", HtmlStyleSheets.select(true, true));
    }

    /** Miuix quotes: no border and the 16dp corner radius of the floor cards (MiuixReaderStyle). */
    @Test
    public void miuixQuotesHaveNoBorderAndCardCorners() throws Exception {
        for (String name : new String[]{"style_miuix_light.css", "style_miuix_dark.css"}) {
            String css = new String(Files.readAllBytes(new File("src/main/assets/html/" + name).toPath()),
                    StandardCharsets.UTF_8);
            Matcher quote = Pattern.compile("div\\.quote\\s*\\{([^}]*)\\}").matcher(css);
            assertTrue(name + " defines div.quote", quote.find());
            String rule = quote.group(1);
            assertTrue(name + " removes the border", rule.contains("border: none"));
            assertTrue(name + " uses the card radius", rule.contains("border-radius: 16px"));
        }
    }
}
