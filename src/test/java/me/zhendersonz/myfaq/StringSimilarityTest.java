package me.zhendersonz.myfaq;

import me.zhendersonz.myfaq.utils.StringSimilarity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringSimilarityTest {

    @Test
    void normalize_basic() {
        assertEquals("como comprar vip", StringSimilarity.normalize("Como Comprar VIP"));
    }

    @Test
    void normalize_lowercase() {
        assertEquals("teste", StringSimilarity.normalize("TESTE"));
    }

    @Test
    void normalize_removesAccents() {
        assertEquals("coracao", StringSimilarity.normalize("coração"));
    }

    @Test
    void normalize_removesPunctuation() {
        assertEquals("como comprar vip", StringSimilarity.normalize("como comprar vip?"));
    }

    @Test
    void normalize_removesMultipleSpaces() {
        assertEquals("como comprar", StringSimilarity.normalize("como   comprar"));
    }

    @Test
    void normalize_trimsSpaces() {
        assertEquals("teste", StringSimilarity.normalize("  teste  "));
    }

    @Test
    void normalize_empty() {
        assertEquals("", StringSimilarity.normalize(""));
    }

    @Test
    void normalize_onlyPunctuation() {
        assertEquals("", StringSimilarity.normalize("?!@#$%"));
    }

    @Test
    void normalize_accentsAndCedilla() {
        assertEquals("voce esta aqui", StringSimilarity.normalize("você está aqui"));
    }

    @Test
    void levenshteinDistance_exactMatch() {
        assertEquals(0, StringSimilarity.levenshteinDistance("comprar", "comprar"));
    }

    @Test
    void levenshteinDistance_oneCharDifferent() {
        assertEquals(1, StringSimilarity.levenshteinDistance("comprar", "compras"));
    }

    @Test
    void levenshteinDistance_completelyDifferent() {
        String a = "abcde";
        String b = "vwxyz";
        assertEquals(5, StringSimilarity.levenshteinDistance(a, b));
    }

    @Test
    void levenshteinDistance_emptyVsString() {
        assertEquals(5, StringSimilarity.levenshteinDistance("", "hello"));
    }

    @Test
    void levenshteinDistance_bothEmpty() {
        assertEquals(0, StringSimilarity.levenshteinDistance("", ""));
    }

    @Test
    void levenshteinSimilarity_exactMatch() {
        assertEquals(1.0, StringSimilarity.levenshteinSimilarity("vip", "vip"), 0.001);
    }

    @Test
    void levenshteinSimilarity_noMatch() {
        assertEquals(0.0, StringSimilarity.levenshteinSimilarity("abc", "xyz"), 0.001);
    }

    @Test
    void levenshteinSimilarity_partial() {
        double sim = StringSimilarity.levenshteinSimilarity("comprar", "compras");
        assertTrue(sim > 0.8 && sim < 1.0);
    }

    @Test
    void levenshteinSimilarity_bothEmpty() {
        assertEquals(1.0, StringSimilarity.levenshteinSimilarity("", ""));
    }

    @Test
    void levenshteinSimilarity_typoSimilar() {
        double sim = StringSimilarity.levenshteinSimilarity(
            StringSimilarity.normalize("comprar"),
            StringSimilarity.normalize("comprá")
        );
        assertTrue(sim >= 0.8);
    }

    @Test
    void normalizeAndMatch_contains() {
        String msg = StringSimilarity.normalize("como comprar vip");
        String kw = StringSimilarity.normalize("comprar vip");
        assertTrue(msg.contains(kw));
    }

    @Test
    void normalizeAndMatch_vipEmPhrase() {
        String msg = StringSimilarity.normalize("como comprar vip no servidor");
        String kw = StringSimilarity.normalize("comprar vip");
        assertTrue(msg.contains(kw));
    }

    @Test
    void normalizeAndMatch_wordByWord() {
        String msg = StringSimilarity.normalize("como comprar um vip");
        String[] kwWords = StringSimilarity.normalize("comprar vip").split("\\s+");
        int matchCount = 0;
        for (String kwWord : kwWords) {
            for (String msgWord : msg.split("\\s+")) {
                if (msgWord.equals(kwWord)) { matchCount++; break; }
            }
        }
        assertEquals(2, matchCount);
    }

    @Test
    void normalizeAndMatch_wordByWord_withExtra() {
        String msg = StringSimilarity.normalize("como comprar um vip novo");
        String[] kwWords = StringSimilarity.normalize("comprar vip").split("\\s+");
        int matchCount = 0;
        for (String kwWord : kwWords) {
            for (String msgWord : msg.split("\\s+")) {
                if (msgWord.equals(kwWord)) { matchCount++; break; }
            }
        }
        assertEquals(2, matchCount);
    }
}
