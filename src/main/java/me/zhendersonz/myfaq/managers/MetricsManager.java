package me.zhendersonz.myfaq.managers;

import me.zhendersonz.myfaq.MyFaq;
import me.zhendersonz.myfaq.models.FAQEntry;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsManager {

    private final MyFaq plugin;
    private final Map<String, Integer> faqCount = new ConcurrentHashMap<>();
    private File metricsFile;

    public MetricsManager(MyFaq plugin) {
        this.plugin = plugin;
    }

    public void load() {
        metricsFile = new File(plugin.getDataFolder(), "metricas.yml");
        if (!metricsFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(metricsFile);
        for (String key : config.getKeys(false)) {
            faqCount.put(key, config.getInt(key, 0));
        }

        for (FAQEntry faq : plugin.getFaqManager().getAllFAQs().values()) {
            faq.setMetricsCount(faqCount.getOrDefault(faq.getId(), 0));
        }
    }

    public void save() {
        if (metricsFile == null) return;

        for (FAQEntry faq : plugin.getFaqManager().getAllFAQs().values()) {
            faqCount.put(faq.getId(), faq.getMetricsCount());
        }

        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : faqCount.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(metricsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Erro ao salvar metricas: " + e.getMessage());
        }
    }

    public void increment(FAQEntry faq) {
        faq.incrementMetrics();
    }

    public int getCount(String faqId) {
        return faqCount.getOrDefault(faqId, 0);
    }

    public Map<String, Integer> getAllCounts() {
        return new ConcurrentHashMap<>(faqCount);
    }
}
