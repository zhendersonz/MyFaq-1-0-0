package me.zhendersonz.myfaq;

import me.zhendersonz.myfaq.models.FAQEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class FAQEntryTest {

    private FAQEntry createBasicEntry() {
        return new FAQEntry("vip",
            Arrays.asList("comprar vip", "adquirir vip"),
            "",
            Collections.singletonList("Compre em www.loja.com"),
            "",
            "mensagem", "", "", "", "", "", "", 0, 0);
    }

    @Test
    void constructorAndGetters() {
        FAQEntry e = createBasicEntry();
        assertEquals("vip", e.getId());
        assertEquals(2, e.getKeywords().size());
        assertEquals("comprar vip", e.getKeywords().get(0));
        assertEquals("Compre em www.loja.com", e.getResponses().get(0));
    }

    @Test
    void getRandomResponse_returnsFromList() {
        List<String> respostas = Arrays.asList("a", "b", "c");
        FAQEntry e = new FAQEntry("test", Collections.singletonList("kw"), "",
            respostas, "", "mensagem", "", "", "", "", "", "", 0, 0);
        String r = e.getRandomResponse();
        assertTrue(respostas.contains(r));
    }

    @Test
    void getRandomResponse_emptyReturnsEmpty() {
        FAQEntry e = new FAQEntry("test", Collections.singletonList("kw"), "",
            Collections.emptyList(), "", "mensagem", "", "", "", "", "", "", 0, 0);
        assertEquals("", e.getRandomResponse());
    }

    @Test
    void isEventActive_noEvent() {
        FAQEntry e = createBasicEntry();
        assertTrue(e.isEventActive());
    }

    @Test
    void isEventActive_withinEvent() {
        long future = System.currentTimeMillis() + 86400000L;
        FAQEntry e = new FAQEntry("event", Collections.singletonList("evento"), "",
            Collections.singletonList("ok"), "", "mensagem",
            "", "", "", "", "", "", System.currentTimeMillis() - 1000, future);
        assertTrue(e.isEventActive());
    }

    @Test
    void isEventActive_expired() {
        FAQEntry e = new FAQEntry("event", Collections.singletonList("evento"), "",
            Collections.singletonList("ok"), "", "mensagem",
            "", "", "", "", "", "", 0, System.currentTimeMillis() - 1000);
        assertFalse(e.isEventActive());
    }

    @Test
    void hasRegex_true() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "test\\d+",
            Collections.singletonList("r"), "", "mensagem", "", "", "", "", "", "", 0, 0);
        assertTrue(e.hasRegex());
    }

    @Test
    void hasRegex_false() {
        assertFalse(createBasicEntry().hasRegex());
    }

    @Test
    void hasCommand_true() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "",
            Collections.singletonList("r"), "say hi", "mensagem",
            "", "", "", "", "", "", 0, 0);
        assertTrue(e.hasCommand());
    }

    @Test
    void hasCommand_false() {
        assertFalse(createBasicEntry().hasCommand());
    }

    @Test
    void hasPermission_true() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "",
            Collections.singletonList("r"), "", "mensagem", "group.vip", "",
            "", "", "", "", 0, 0);
        assertTrue(e.hasPermission());
    }

    @Test
    void hasPermission_false() {
        assertFalse(createBasicEntry().hasPermission());
    }

    @Test
    void hasClickable_textOnly() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "",
            Collections.singletonList("r"), "", "mensagem", "", "",
            "Clique aqui", "/cmd", "", "", 0, 0);
        assertTrue(e.hasClickable());
    }

    @Test
    void hasClickable_false() {
        assertFalse(createBasicEntry().hasClickable());
    }

    @Test
    void hasClickableUrl_true() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "",
            Collections.singletonList("r"), "", "mensagem", "", "",
            "Clique", "", "https://url.com", "", 0, 0);
        assertTrue(e.hasClickableUrl());
    }

    @Test
    void hasClickableUrl_false() {
        assertFalse(createBasicEntry().hasClickableUrl());
    }

    @Test
    void hasSound_true() {
        FAQEntry e = new FAQEntry("x", Collections.singletonList("kw"), "",
            Collections.singletonList("r"), "", "mensagem", "", "", "", "", "", "BLOCK_NOTE_BLOCK_PLING", 0, 0);
        assertTrue(e.hasSound());
    }

    @Test
    void hasSound_false() {
        assertFalse(createBasicEntry().hasSound());
    }

    @Test
    void incrementMetrics() {
        FAQEntry e = createBasicEntry();
        assertEquals(0, e.getMetricsCount());
        e.incrementMetrics();
        assertEquals(1, e.getMetricsCount());
        e.incrementMetrics();
        assertEquals(2, e.getMetricsCount());
    }

    @Test
    void getDisplayInfo_containsId() {
        String info = createBasicEntry().getDisplayInfo();
        assertTrue(info.contains("vip"));
        assertTrue(info.contains("comprar vip"));
        assertTrue(info.contains("Ativacoes"));
    }
}
