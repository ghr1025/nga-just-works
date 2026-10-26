package gov.anzong.androidnga.core.corebuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.anzong.androidnga.core.data.AttachmentData;
import gov.anzong.androidnga.core.data.HtmlData;

public class HtmlAttachmentBuilderTest {

    private static HtmlData withImageAttachment(boolean miuix) {
        AttachmentData attachment = new AttachmentData();
        attachment.setAttachUrl("mon_202610/26/abc.jpg");
        HtmlData data = new HtmlData("body");
        data.setAttachmentsPrefix("https://img.nga.178.com/attachments");
        data.setAttachmentList(Collections.singletonList(attachment));
        data.setMiuixStyle(miuix);
        return data;
    }

    @Test
    public void classicKeepsTheBorderedTable() {
        List<String> images = new ArrayList<>();
        String html = new HtmlAttachmentBuilder().build(withImageAttachment(false), images).toString();
        assertTrue(html.contains("border:1px solid #b9986e"));
        assertTrue(html.contains("附件<hr/>"));
        assertFalse(html.contains("class='attach'"));
        assertEquals(1, images.size());
    }

    @Test
    public void miuixUsesStyleSheetClassesWithoutInlineBorder() {
        List<String> images = new ArrayList<>();
        String html = new HtmlAttachmentBuilder().build(withImageAttachment(true), images).toString();
        assertTrue(html.contains("<table class='attach'>"));
        assertTrue(html.contains("class='attach-title'"));
        assertTrue(html.contains("class='attach-show'"));
        assertFalse(html.contains("border:1px solid"));
        assertFalse(html.contains("<hr/>"));
        // the show button still loads the image the same way
        assertTrue(html.contains("onclick='displayImg(1,"));
        assertEquals(1, images.size());
    }
}
