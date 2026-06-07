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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FAQManager {

    private final MyFaq plugin;
    private final Map<String, FAQEntry> faqs = new ConcurrentHashMap<>();
    private double sensibilidade;
    private List<String> antiLoop = new ArrayList<>();
    private Set<String> stopWords = new HashSet<>();
    private List<String> boasVindas = new ArrayList<>();
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
        if (this.antiLoop == null) this.antiLoop = new ArrayList<>();
        
        List<String> stopList = config.getStringList("stop-words");
        if (stopList == null || stopList.isEmpty()) {
            this.stopWords = new HashSet<>(Arrays.asList("como", "fazer", "para", "quero", "queria", "onde"));
        } else {
            this.stopWords = stopList.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }

        this.boasVindas = config.getStringList("boas-vindas");
        if (this.boasVindas == null) this.boasVindas = new ArrayList<>();

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

        FAQEntry bestMatch = null;
        double highestScore = -1.0;

        for (FAQEntry faq : faqs.values()) {
            if (!faq.isEventActive()) continue;

            MatchResult result = matchFAQDetailed(normalized, faq);
            if (result.matched && result.score > highestScore) {
                highestScore = result.score;
                bestMatch = faq;
            }
        }

        if (highestScore < sensibilidade) return null;

        return bestMatch;
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
            
            double baseScore = similarity;
            if (matched) {
                switch (method) {
                    case "regex":
                    case "contains":
                        baseScore = 1.0;
                        break;
                    case "contains-clean":
                        baseScore = 0.98;
                        break;
                    case "janela-palavras":
                        baseScore = Math.max(similarity, 0.95);
                        break;
                    default:
                        baseScore = similarity;
                        break;
                }
            }
            this.score = matched ? baseScore : 0.0;
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

        String cleanedMsg = StringSimilarity.cleanStopWords(normalized, stopWords);

        for (String keyword : faq.getKeywords()) {
            String kw = StringSimilarity.normalize(keyword);
            if (kw.isEmpty()) continue;

            if (normalized.contains(kw)) {
                return new MatchResult(true, "contains", keyword, 1.0);
            }

            String cleanedKw = StringSimilarity.cleanStopWords(kw, stopWords);
            if (!cleanedKw.isEmpty() && !cleanedMsg.isEmpty() && cleanedMsg.contains(cleanedKw)) {
                return new MatchResult(true, "contains-clean", keyword, 1.0);
            }
        }

        if (sensibilidade < 1.0) {
            // Trava para mensagens muito curtas/vazias após limpeza
            if (cleanedMsg.isEmpty() || (cleanedMsg.split("\\s+").length <= 1 && normalized.length() < 10)) {
                return new MatchResult(false, "mensagem-curta-significado", "", 0.0);
            }

            for (String keyword : faq.getKeywords()) {
                String kw = StringSimilarity.normalize(keyword);
                if (kw.isEmpty()) continue;

                String cleanedKw = StringSimilarity.cleanStopWords(kw, stopWords);
                String[] kwWords = kw.split("\\s+");
                String[] msgWords = normalized.split("\\s+");

                // Janela deslizante baseada em palavras
                if (msgWords.length >= kwWords.length) {
                    for (int i = 0; i <= msgWords.length - kwWords.length; i++) {
                        StringBuilder windowBuilder = new StringBuilder();
                        for (int j = 0; j < kwWords.length; j++) {
                            windowBuilder.append(msgWords[i + j]).append(" ");
                        }
                        String window = windowBuilder.toString().trim();
                        double sim = StringSimilarity.jaroWinklerSimilarity(window, kw);
                        
                        if (sim >= sensibilidade) {
                            return new MatchResult(true, "janela-palavras", keyword, sim);
                        }
                    }
                }

                // Similaridade global aprimorada (usando versões limpas se possível)
                double simJaro = StringSimilarity.jaroWinklerSimilarity(
                    cleanedMsg.isEmpty() ? normalized : cleanedMsg,
                    cleanedKw.isEmpty() ? kw : cleanedKw
                );
                
                if (simJaro >= sensibilidade) {
                    return new MatchResult(true, "jaro-winkler-global", keyword, simJaro);
                }

                // Similaridade por palavras individuais (fuzzy match)
                int matchCount = 0;
                int significantKwWords = 0;
                for (String kwWord : kwWords) {
                    if (kwWord.length() <= 2) continue; // Nao conta palavras curtas
                    significantKwWords++;
                    for (String msgWord : msgWords) {
                        double wordSim = StringSimilarity.jaroWinklerSimilarity(msgWord, kwWord);
                        if (wordSim >= 0.85) { // Rigoroso para palavras individuais
                            matchCount++;
                            break;
                        }
                    }
                }
                
                if (significantKwWords > 0) {
                    double ratio = (double) matchCount / significantKwWords;
                    if (ratio >= sensibilidade) {
                        return new MatchResult(true, "fuzzy-palavras", keyword, ratio);
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
