package me.zhendersonz.myfaq.utils;

import me.zhendersonz.myfaq.models.FAQEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static String applyPlaceholders(String message, Player player, FAQEntry faq) {
        String result = message;
        result = result.replace("%player%", player.getName());
        result = result.replace("%player_display%", player.getDisplayName());
        result = result.replace("%faq_id%", faq != null ? faq.getId() : "");
        result = result.replace("%uuid%", player.getUniqueId().toString());
        return result;
    }

    public static Component toComponent(String text) {
        return LegacyComponentSerializer.legacySection()
            .deserialize(text.replace('&', '§'));
    }

    public static Component buildClickable(String text, String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        return toComponent(text)
            .clickEvent(ClickEvent.runCommand("/" + cmd));
    }

    public static Component buildClickableUrl(String text, String url) {
        String finalUrl = url;
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://" + finalUrl;
        }
        return toComponent(text)
            .clickEvent(ClickEvent.openUrl(finalUrl));
    }

    public static void sendResponse(Player player, FAQEntry faq, String textContent) {
        String processed = applyPlaceholders(textContent, player, faq);
        String tipo = faq.getResponseType() != null ? faq.getResponseType() : "mensagem";

        Component msg;
        if (faq.hasClickable()) {
            String clickText = applyPlaceholders(faq.getClickableText(), player, faq);
            Component resposta = toComponent(processed);
            Component clicavel;
            if (faq.hasClickableUrl()) {
                String url = applyPlaceholders(faq.getClickableUrl(), player, faq);
                clicavel = buildClickableUrl("\n" + clickText, url);
            } else {
                String clickCmd = applyPlaceholders(faq.getClickableCommand(), player, faq);
                clicavel = buildClickable("\n" + clickText, clickCmd);
            }
            msg = resposta.append(clicavel);
        } else {
            msg = toComponent(processed);
        }

        switch (tipo.toLowerCase()) {
            case "title":
                player.sendTitle(
                    LegacyComponentSerializer.legacySection().serialize(msg),
                    "",
                    10, 70, 20
                );
                player.sendMessage(msg);
                break;
            case "chat":
                player.getServer().broadcast(msg);
                break;
            default:
                player.sendMessage(msg);
                break;
        }

        if (faq.hasSound()) {
            try {
                Sound sound = Sound.valueOf(faq.getSound().toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static void executeCommand(FAQEntry faq, Player player) {
        if (!faq.hasCommand()) return;
        String cmd = applyPlaceholders(faq.getCommand(), player, faq);
        player.getServer().dispatchCommand(
            player.getServer().getConsoleSender(),
            cmd
        );
    }

    public static String formatLog(String template, Player player, FAQEntry faq, String message) {
        String result = template;
        result = result.replace("%data%", new java.text.SimpleDateFormat("dd/MM/yyyy")
            .format(new java.util.Date()));
        result = result.replace("%hora%", new java.text.SimpleDateFormat("HH:mm:ss")
            .format(new java.util.Date()));
        result = result.replace("%player%", player.getName());
        result = result.replace("%faq-id%", faq.getId());
        result = result.replace("%mensagem%", message);
        return result;
    }
}
