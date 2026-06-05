package me.zhendersonz.myfaq.listeners;

import me.zhendersonz.myfaq.MyFaq;
import me.zhendersonz.myfaq.models.FAQEntry;
import me.zhendersonz.myfaq.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ChatListener implements Listener {

    private final MyFaq plugin;

    public ChatListener(MyFaq plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        if (!plugin.getCooldownManager().isToggled(player)) return;
        if (player.hasPermission("myfaq.ignorar")) return;
        if (player.hasMetadata("NPC")) return;

        String message = event.getMessage();
        FAQEntry matched = plugin.getFaqManager().match(message);

        if (matched == null) return;

        if (!plugin.getCooldownManager().canActivate(player, matched.getId())) {
            long remaining = plugin.getCooldownManager().getPlayerRemaining(player);
            player.sendMessage(MessageUtil.toComponent(
                plugin.getPrefixo() + " §cAguarde " + remaining + " segundos para perguntar novamente."));
            return;
        }

        plugin.getCooldownManager().setCooldown(player, matched.getId());

        if (matched.hasPermission() && !player.hasPermission(matched.getPermission())) {
            if (matched.getPermissionDenied() != null && !matched.getPermissionDenied().isEmpty()) {
                player.sendMessage(
                    MessageUtil.toComponent(
                        MessageUtil.applyPlaceholders(
                            matched.getPermissionDenied(), player, matched
                        )
                    )
                );
            }
            return;
        }

        String response = matched.getRandomResponse();

        int delay = plugin.getConfig().getInt("delay-entre-faqs", 500);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            MessageUtil.sendResponse(player, matched, response);

            if (matched.hasCommand()) {
                MessageUtil.executeCommand(matched, player);
            }

            plugin.getMetricsManager().increment(matched);

            if (plugin.getFaqManager().isLogAtivacoes()) {
                logAtivacao(player, matched, message);
            }

            if (plugin.isNotificarStaff()) {
                notificarStaff(player, message);
            }
        }, delay / 50);
    }

    private void logAtivacao(Player player, FAQEntry faq, String message) {
        String logText = MessageUtil.formatLog(
            plugin.getFaqManager().getLogFormato(),
            player, faq, message
        );

        File logFile = new File(plugin.getDataFolder(),
            plugin.getFaqManager().getLogArquivo());

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFile, true))) {
            writer.write(logText);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Erro ao escrever log: " + e.getMessage());
        }
    }

    private void notificarStaff(Player player, String message) {
        String msg = plugin.getNotificarStaffMsg()
            .replace("%player%", player.getName())
            .replace("%mensagem%", message);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("myfaq.notify")) {
                online.sendMessage(MessageUtil.toComponent(msg));
            }
        }
    }
}
