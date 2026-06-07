package me.zhendersonz.myfaq.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class StringSimilarity {

    public static double levenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) distance / maxLen);
    }

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    public static double jaroWinklerSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int searchRange = Math.max(0, Math.max(len1, len2) / 2 - 1);
        boolean[] hash1 = new boolean[len1];
        boolean[] hash2 = new boolean[len2];

        int common = 0;
        for (int i = 0; i < len1; i++) {
            for (int j = Math.max(0, i - searchRange); j < Math.min(len2, i + searchRange + 1); j++) {
                if (s1.charAt(i) == s2.charAt(j) && !hash2[j]) {
                    hash1[i] = true;
                    hash2[j] = true;
                    common++;
                    break;
                }
            }
        }
        if (common == 0) return 0.0;

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (hash1[i]) {
                while (!hash2[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions++;
                k++;
            }
        }
        transpositions /= 2.0;

        double jaro = ((double) common / len1 + (double) common / len2 + (common - transpositions) / common) / 3.0;
        
        // Winkler correction
        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + 0.1 * prefix * (1.0 - jaro);
    }

    public static String normalize(String str) {
        if (str == null) return "";
        return java.text.Normalizer.normalize(str.toLowerCase(), java.text.Normalizer.Form.NFD)
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    public static String cleanStopWords(String str, Set<String> stopWords) {
        if (str == null || str.isEmpty()) return "";
        if (stopWords == null || stopWords.isEmpty()) return str;
        return Arrays.stream(str.split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word.toLowerCase()))
            .collect(Collectors.joining(" "));
    }
}
