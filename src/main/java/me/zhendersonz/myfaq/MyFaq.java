package me.zhendersonz.myfaq;

import me.zhendersonz.myfaq.commands.FAQCommand;
import me.zhendersonz.myfaq.listeners.ChatListener;
import me.zhendersonz.myfaq.listeners.PlayerListener;
import me.zhendersonz.myfaq.managers.CooldownManager;
import me.zhendersonz.myfaq.managers.FAQManager;
import me.zhendersonz.myfaq.managers.MetricsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MyFaq extends JavaPlugin {

    private static MyFaq instance;
    private FAQManager faqManager;
    private CooldownManager cooldownManager;
    private MetricsManager metricsManager;
    private String prefixo;
    private boolean notificarStaff;
    private String notificarStaffMsg;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.faqManager = new FAQManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.metricsManager = new MetricsManager(this);

        faqManager.loadFAQs();
        metricsManager.load();

        this.prefixo = getConfig().getString("prefixo", "§7[§aMyFaq§7]§r");
        this.notificarStaff = getConfig().getBoolean("notificar-staff", true);
        this.notificarStaffMsg = getConfig().getString("notificar-staff-msg",
            "§7[§aFAQ§7] §f%player% §7perguntou: §f%mensagem%");

        getCommand("faq").setExecutor(new FAQCommand(this));
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("MyFaq ativado com sucesso!");

        faqManager.limparLogSeNecessario();
        iniciarLimpezaAgendada();
    }

    private void iniciarLimpezaAgendada() {
        int dias = getConfig().getInt("limpar-log-dias", 0);
        if (dias <= 0) return;

        long ticks = 20L * 60L * 60L * 6L; // 6 horas
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            faqManager.limparLogSeNecessario();
        }, ticks, ticks);
    }

    @Override
    public void onDisable() {
        metricsManager.save();
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("MyFaq desativado.");
    }

    public static MyFaq getInstance() {
        return instance;
    }

    public FAQManager getFaqManager() {
        return faqManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public MetricsManager getMetricsManager() {
        return metricsManager;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public boolean isNotificarStaff() {
        return notificarStaff;
    }

    public String getNotificarStaffMsg() {
        return notificarStaffMsg;
    }

}
