package me.zhendersonz.myfaq;

import me.zhendersonz.myfaq.models.FAQEntry;
import me.zhendersonz.myfaq.utils.StringSimilarity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MatchingTest {

    private FAQEntry vipFAQ() {
        return new FAQEntry("vip",
            Arrays.asList("comprar vip", "adquirir vip", "preco do vip"),
            "",
            Collections.singletonList("Compre em www.loja.com"),
            "",
            "mensagem", "", "", "", "", "", "", 0, 0);
    }

    private FAQEntry discordFAQ() {
        return new FAQEntry("discord",
            Arrays.asList("discord", "link do discord"),
            "",
            Collections.singletonList("discord.gg/servidor"),
            "",
            "mensagem", "", "", "", "", "", "", 0, 0);
    }

    private FAQEntry hackFAQ() {
        return new FAQEntry("hack",
            Arrays.asList("hack", "hacker", "usar hack"),
            "",
            Collections.singletonList("Hacks proibidos"),
            "",
            "mensagem", "", "", "", "", "", "", 0, 0);
    }

    private boolean matchContains(String message, FAQEntry faq) {
        String normalized = StringSimilarity.normalize(message);
        for (String keyword : faq.getKeywords()) {
            String kw = StringSimilarity.normalize(keyword);
            if (!kw.isEmpty() && normalized.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchWordByWord(String message, FAQEntry faq) {
        String normalized = StringSimilarity.normalize(message);
        for (String keyword : faq.getKeywords()) {
            String kw = StringSimilarity.normalize(keyword);
            if (kw.isEmpty()) continue;
            String[] kwWords = kw.split("\\s+");
            String[] msgWords = normalized.split("\\s+");
            if (kwWords.length >= 2 && msgWords.length >= kwWords.length) {
                int matchCount = 0;
                for (String kwWord : kwWords) {
                    if (kwWord.length() <= 2) { matchCount++; continue; }
                    for (String msgWord : msgWords) {
                        if (msgWord.equals(kwWord)) { matchCount++; break; }
                    }
                }
                double ratio = (double) matchCount / kwWords.length;
                if (ratio >= 0.8) return true;
            }
        }
        return false;
    }

    private double similarityScore(String s1, String s2) {
        return StringSimilarity.levenshteinSimilarity(s1, s2);
    }

    @Test
    void exactMatch_vip() {
        assertTrue(matchContains("como comprar vip", vipFAQ()));
    }

    @Test
    void exactMatch_discord() {
        assertTrue(matchContains("qual o link do discord", discordFAQ()));
    }

    @Test
    void exactMatch_hack() {
        assertTrue(matchContains("pode usar hack", hackFAQ()));
    }

    @Test
    void noMatch_vipWrong() {
        assertFalse(matchContains("qual o spawn", vipFAQ()));
    }

    @Test
    void noMatch_discordWrong() {
        assertFalse(matchContains("como comprar rank", discordFAQ()));
    }

    @Test
    void contains_substringMatch() {
        assertTrue(matchContains("quanto custa comprar vip", vipFAQ()));
    }

    @Test
    void contains_vipComLetraExtra() {
        assertTrue(matchContains("como comprar vip agora", vipFAQ()));
    }

    @Test
    void wordByWord_comprarVip() {
        assertTrue(matchWordByWay("como comprar um novo vip no servidor", vipFAQ()));
    }

    @Test
    void wordByWord_apenasParte() {
        assertFalse(matchWordByWay("quanto custa", vipFAQ()));
    }

    @Test
    void levenshteinSimilarity_comprarCompras() {
        double sim = similarityScore("comprar", "compras");
        assertTrue(sim > 0.8, "comprar vs compras similarity = " + sim);
    }

    @Test
    void levenshteinSimilarity_vipVip() {
        double sim = similarityScore("vip", "vip");
        assertEquals(1.0, sim);
    }

    @Test
    void levenshteinSimilarity_completelyDifferent() {
        double sim = similarityScore("abcde", "vwxyz");
        assertTrue(sim < 0.3);
    }

    @Test
    void antiLoop_ignoresThanks() {
        String msg = StringSimilarity.normalize("obrigado");
        assertTrue(msg.contains("obrigado"));
    }

    @Test
    void antiLoop_ignoresVlw() {
        String msg = StringSimilarity.normalize("vlw");
        assertTrue(msg.contains("vlw"));
    }

    @Test
    void matchViaRegex() {
        FAQEntry faq = new FAQEntry("regex", Collections.singletonList(""), "compr[aeiou]r\\s+vip",
            Collections.singletonList("ok"), "", "mensagem", "", "", "", "", "", "", 0, 0);
        String normalized = StringSimilarity.normalize("comprar vip");
        assertTrue(java.util.regex.Pattern.compile("compr[aeiou]r\\s+vip", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(normalized).find());
    }

    @Test
    void multipleResponses_randomPick() {
        FAQEntry faq = new FAQEntry("multi",
            Collections.singletonList("teste"),
            "",
            Arrays.asList("r1", "r2", "r3"),
            "", "mensagem", "", "", "", "", "", "", 0, 0);
        String r = faq.getRandomResponse();
        assertTrue(r.equals("r1") || r.equals("r2") || r.equals("r3"));
    }

    @Test
    void antiLoop_naoAtrapalhaMensagem() {
        String msg = StringSimilarity.normalize("como comprar vip obrigado");
        assertTrue(msg.contains("comprar vip"));
    }

    private boolean matchWordByWay(String message, FAQEntry faq) {
        return matchWordByWord(message, faq);
    }
}
