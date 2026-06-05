package me.zhendersonz.myfaq.managers;

import me.zhendersonz.myfaq.MyFaq;
import me.zhendersonz.myfaq.models.FAQEntry;
import me.zhendersonz.myfaq.utils.StringSimilarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FAQManager {

    private final MyFaq plugin;
    private final Map<String, FAQEntry> faqs = new LinkedHashMap<>();
    private double sensibilidade;
    private List<String> antiLoop;
    private List<String> boasVindas;
    private boolean logAtivacoes;
    private String logArquivo;
    private String logFormato;

    public FAQManager(MyFaq plugin) {
        this.plugin = plugin;
    }

    public void loadFAQs() {
        faqs.clear();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.sensibilidade = config.getDouble("sensibilidade", 0.8);
        this.antiLoop = config.getStringList("anti-loop");
        this.boasVindas = config.getStringList("boas-vindas");
        this.logAtivacoes = config.getBoolean("log-ativacoes", true);
        this.logArquivo = config.getString("log-arquivo", "faq-log.txt");
        this.logFormato = config.getString("log-formato",
            "[%data% %hora%] %player% acionou '%faq-id%' com a mensagem: '%mensagem%'");

        loadFromConfig(config);
    }

    private void loadFromConfig(FileConfiguration config) {
        ConfigurationSection faqSection = config.getConfigurationSection("faqs");
        if (faqSection == null) return;

        for (String id : faqSection.getKeys(false)) {
            ConfigurationSection section = faqSection.getConfigurationSection(id);
            if (section == null) continue;

            List<String> keywords = section.getStringList("palavras");
            String regex = section.getString("regex", "");
            List<String> responses = section.getStringList("respostas");
            if (responses.isEmpty()) {
                String single = section.getString("resposta");
                if (single != null) {
                    responses = Collections.singletonList(single);
                } else {
                    plugin.getLogger().warning("FAQ '" + id + "' sem respostas, pulando.");
                    continue;
                }
            }

            String command = section.getString("comando", "");
            String responseType = section.getString("tipo-resposta", "mensagem");
            String permission = section.getString("permissao", "");
            String permissionDenied = section.getString("permissao-negada", "");
            String clickableText = section.getString("mensagem-clicavel.texto", "");
            String clickableCommand = section.getString("mensagem-clicavel.comando", "");
            String clickableUrl = section.getString("mensagem-clicavel.url", "");
            String sound = section.getString("som", "");
            long eventStart = parseTime(section.getString("evento-inicio", ""));
            long eventEnd = parseTime(section.getString("evento-fim", ""));

            if (keywords.isEmpty() && regex.isEmpty()) {
                plugin.getLogger().warning("FAQ '" + id + "' sem palavras nem regex, pulando.");
                continue;
            }

            FAQEntry entry = new FAQEntry(id, keywords, regex, responses,
                command, responseType, permission, permissionDenied,
                clickableText, clickableCommand, clickableUrl, sound,
                eventStart, eventEnd);

            faqs.put(id, entry);
        }

        plugin.getLogger().info(faqs.size() + " FAQs carregadas.");
    }

    private long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public FAQEntry match(String message) {
        if (message == null || message.isEmpty()) return null;

        String normalized = StringSimilarity.normalize(message);
        if (normalized.isEmpty()) return null;

        for (String word : antiLoop) {
            if (normalized.contains(word.toLowerCase())) {
                return null;
            }
        }

        for (FAQEntry faq : faqs.values()) {
            if (!faq.isEventActive()) continue;

            if (matchFAQ(normalized, faq)) {
                return faq;
            }
        }

        return null;
    }

    public static class MatchResult {
        public final boolean matched;
        public final String method;
        public final String keyword;
        public final double similarity;
        public final double score;

        public MatchResult(boolean matched, String method, String keyword, double similarity) {
            this.matched = matched;
            this.method = method;
            this.keyword = keyword;
            this.similarity = similarity;
            this.score = matched ? similarity : 0.0;
        }

        public double getScore() {
            return score;
        }

        public String getDebugString(String faqId) {
            if (!matched) return "§7[" + faqId + "] §cnao matchou §7(score: §e"
                + String.format("%.2f", score) + "§7)";
            return "§a[" + faqId + "] §fmatch via: §e" + method
                + " §7(kw: '" + keyword + "'"
                + (similarity < 1.0 ? ", sim: " + String.format("%.2f", similarity) : "")
                + " §7score: " + String.format("%.2f", score)
                + ")";
        }
    }

    private boolean matchFAQ(String normalized, FAQEntry faq) {
        return matchFAQDetailed(normalized, faq).matched;
    }

    public MatchResult matchFAQDetailed(String normalized, FAQEntry faq) {
        if (faq.hasRegex()) {
            try {
                Pattern p = Pattern.compile(faq.getRegex(), Pattern.CASE_INSENSITIVE);
                if (p.matcher(normalized).find()) {
                    return new MatchResult(true, "regex", faq.getRegex(), 1.0);
                }
            } catch (PatternSyntaxException ignored) {
            }
        }

        for (String keyword : faq.getKeywords()) {
            String kw = StringSimilarity.normalize(keyword);
            if (kw.isEmpty()) continue;

            if (normalized.contains(kw)) {
                return new MatchResult(true, "contains", keyword, 1.0);
            }
        }

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
                if (ratio >= 0.8) {
                    return new MatchResult(true, "palavras", keyword, ratio);
                }
            }
        }

        if (sensibilidade < 1.0) {
            for (String keyword : faq.getKeywords()) {
                String kw = StringSimilarity.normalize(keyword);
                if (kw.isEmpty() || kw.length() <= 2) continue;

                String[] kwWords = kw.split("\\s+");
                String[] msgWords = normalized.split("\\s+");

                if (kwWords.length >= 2 && msgWords.length >= kwWords.length) {
                    int matchCount = 0;
                    for (String kwWord : kwWords) {
                        if (kwWord.length() <= 2) { matchCount++; continue; }
                        for (String msgWord : msgWords) {
                            double sim = StringSimilarity.levenshteinSimilarity(msgWord, kwWord);
                            if (sim >= sensibilidade) { matchCount++; break; }
                        }
                    }
                    double ratio = (double) matchCount / kwWords.length;
                    if (ratio >= sensibilidade) {
                        return new MatchResult(true, "similaridade", keyword, ratio);
                    }
                }

                double similarity = StringSimilarity.levenshteinSimilarity(
                    normalized.length() > 100 ? normalized.substring(0, 100) : normalized,
                    kw
                );
                if (similarity >= sensibilidade) {
                    return new MatchResult(true, "similaridade-global", keyword, similarity);
                }

                if (normalized.length() > kw.length()) {
                    for (int i = 0; i <= normalized.length() - kw.length(); i++) {
                        String window = normalized.substring(i, i + kw.length());
                        double sim = StringSimilarity.levenshteinSimilarity(window, kw);
                        if (sim >= sensibilidade) {
                            return new MatchResult(true, "janela", keyword, sim);
                        }
                    }
                }
            }
        }

        return new MatchResult(false, "", "", 0.0);
    }

    public MatchResult matchDebug(String message) {
        if (message == null || message.isEmpty())
            return new MatchResult(false, "", "", 0.0);

        String normalized = StringSimilarity.normalize(message);
        if (normalized.isEmpty())
            return new MatchResult(false, "", "", 0.0);

        for (String word : antiLoop) {
            if (normalized.contains(word.toLowerCase())) {
                return new MatchResult(false, "anti-loop: '" + word + "'", "", 0.0);
            }
        }

        for (FAQEntry faq : faqs.values()) {
            if (!faq.isEventActive()) continue;

            MatchResult result = matchFAQDetailed(normalized, faq);
            if (result.matched) {
                return result;
            }
        }

        return new MatchResult(false, "nenhuma FAQ matchou", "", 0.0);
    }

    public FAQEntry getFAQ(String id) {
        return faqs.get(id);
    }

    public Map<String, FAQEntry> getAllFAQs() {
        return Collections.unmodifiableMap(faqs);
    }

    public double getSensibilidade() {
        return sensibilidade;
    }

    public List<String> getAntiLoop() {
        return antiLoop;
    }

    public List<String> getBoasVindas() {
        return boasVindas;
    }

    public boolean isLogAtivacoes() {
        return logAtivacoes;
    }

    public String getLogArquivo() {
        return logArquivo;
    }

    public String getLogFormato() {
        return logFormato;
    }

    public int getTotalFAQs() {
        return faqs.size();
    }

    public void limparLogSeNecessario() {
        int dias = plugin.getConfig().getInt("limpar-log-dias", 0);
        if (dias <= 0) return;

        File logFile = new File(plugin.getDataFolder(), logArquivo);
        if (!logFile.exists()) return;

        long idadeMax = dias * 24L * 60L * 60L * 1000L;
        long idade = System.currentTimeMillis() - logFile.lastModified();

        if (idade > idadeMax) {
            try {
                Files.write(logFile.toPath(), new byte[0]);
                plugin.getLogger().info("Log limpo automaticamente (" + dias + " dias).");
            } catch (IOException e) {
                plugin.getLogger().warning("Erro ao limpar log: " + e.getMessage());
            }
        }
    }
}
