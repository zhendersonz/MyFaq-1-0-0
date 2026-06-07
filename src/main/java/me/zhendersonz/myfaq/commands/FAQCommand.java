package me.zhendersonz.myfaq.commands;

import me.zhendersonz.myfaq.MyFaq;
import me.zhendersonz.myfaq.models.FAQEntry;
import me.zhendersonz.myfaq.utils.MessageUtil;
import me.zhendersonz.myfaq.managers.FAQManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FAQCommand implements CommandExecutor, TabCompleter {

    private final MyFaq plugin;

    public FAQCommand(MyFaq plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("lista", "toggle"));
            if (sender.hasPermission("myfaq.admin")) {
                subcommands.addAll(Arrays.asList("admin", "top", "recarregar", "test", "clear"));
            }
            return subcommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("test") && sender.hasPermission("myfaq.admin")) {
                return plugin.getFaqManager().getAllFAQs().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("clear") && sender.hasPermission("myfaq.admin")) {
                return null; // Sugere jogadores online
            }
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cComando apenas para jogadores.");
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage("§6§lMyFaq §7- §fPerguntas Frequentes");
            player.sendMessage(" §7Use §a/faq lista §7para ver perguntas reconhecidas");
            player.sendMessage(" §7Use §a/faq toggle §7para ativar/desativar");
            if (player.hasPermission("myfaq.admin")) {
                player.sendMessage(" §7Use §a/faq admin §7para administrar");
                player.sendMessage(" §7Use §a/faq top §7para ranking de ativacoes");
                player.sendMessage(" §7Use §a/faq recarregar §7para recarregar config");
                player.sendMessage(" §7Use §a/faq test <msg> §7para testar matching");
                player.sendMessage(" §7Use §a/faq clear <jogador> §7para limpar cooldown");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin":
                return handleAdmin(sender);
            case "recarregar":
            case "reload":
                return handleReload(sender);
            case "toggle":
                return handleToggle(sender);
            case "test":
            case "testar":
                return handleTest(sender, args);
            case "lista":
            case "list":
                return handleLista(sender);
            case "top":
                return handleTop(sender);
            case "clear":
            case "limpar":
                return handleClear(sender, args);
            default:
                sender.sendMessage("§cComando desconhecido. Use: /faq [lista|toggle|admin|top|clear|recarregar|test]");
                return true;
        }
    }

    private boolean handleAdmin(CommandSender sender) {
        if (!sender.hasPermission("myfaq.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        sender.sendMessage("§6§lMyFaq §7- §fAdministracao");
        sender.sendMessage(" §7FAQs carregadas: §e" + plugin.getFaqManager().getTotalFAQs());
        sender.sendMessage(" §7Sensibilidade: §e" + String.format("%.2f", plugin.getFaqManager().getSensibilidade()));
        sender.sendMessage("");

        for (FAQEntry faq : plugin.getFaqManager().getAllFAQs().values()) {
            sender.sendMessage(faq.getDisplayInfo());
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("myfaq.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        plugin.getFaqManager().reloadConfig();
        plugin.getCooldownManager().reload();
        sender.sendMessage("§aMyFaq recarregado com sucesso!");
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("myfaq.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUse: /faq test <mensagem>");
            return true;
        }

        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msg.append(args[i]).append(" ");
        }
        String message = msg.toString().trim();

        sender.sendMessage("§6§lMyFaq §7- §fTeste de Matching");
        sender.sendMessage(" §7Mensagem: §f" + message);
        sender.sendMessage("");

        String normalized = me.zhendersonz.myfaq.utils.StringSimilarity.normalize(message);
        sender.sendMessage(" §7Normalizada: §f" + normalized);
        sender.sendMessage("");

        java.util.List<FAQEntry> allFAQs = new java.util.ArrayList<>(plugin.getFaqManager().getAllFAQs().values());
        allFAQs.sort((a, b) -> {
            FAQManager.MatchResult ra = plugin.getFaqManager().matchFAQDetailed(normalized, a);
            FAQManager.MatchResult rb = plugin.getFaqManager().matchFAQDetailed(normalized, b);
            return Double.compare(rb.getScore(), ra.getScore());
        });

        for (FAQEntry faq : allFAQs) {
            FAQManager.MatchResult result =
                plugin.getFaqManager().matchFAQDetailed(normalized, faq);
            sender.sendMessage(result.getDebugString(faq.getId()));
        }

        return true;
    }

    private boolean handleLista(CommandSender sender) {
        java.util.Map<String, FAQEntry> faqs = plugin.getFaqManager().getAllFAQs();
        sender.sendMessage("§6§lMyFaq §7- §fPerguntas que reconheco:");
        sender.sendMessage("");

        StringBuilder line = new StringBuilder(" §7");
        int count = 0;
        for (String id : faqs.keySet()) {
            line.append("§a").append(id).append("§7, ");
            count++;
            if (count % 5 == 0) {
                sender.sendMessage(line.toString());
                line = new StringBuilder(" §7");
            }
        }
        if (!line.toString().equals(" §7")) {
            sender.sendMessage(line.substring(0, line.length() - 2));
        }

        sender.sendMessage("");
        sender.sendMessage(" §7Total: §e" + faqs.size() + " §7perguntas cadastradas.");
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        if (!sender.hasPermission("myfaq.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        java.util.List<FAQEntry> sorted = new java.util.ArrayList<>(plugin.getFaqManager().getAllFAQs().values());
        sorted.sort((a, b) -> Integer.compare(b.getMetricsCount(), a.getMetricsCount()));

        sender.sendMessage("§6§lMyFaq §7- §fTop FAQs mais ativadas:");
        sender.sendMessage("");

        int pos = 1;
        for (FAQEntry faq : sorted) {
            if (pos > 10) break;
            String medal = pos == 1 ? "§e#1" : pos == 2 ? "§7#2" : pos == 3 ? "§6#3" : " §f#" + pos;
            sender.sendMessage(" " + medal + " §a" + faq.getId() + " §7- §e" + faq.getMetricsCount() + " §7ativacoes");
            pos++;
        }

        if (sorted.isEmpty()) {
            sender.sendMessage(" §7Nenhuma ativacao registrada.");
        }

        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("myfaq.admin")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUse: /faq clear <jogador>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJogador nao encontrado ou offline.");
            return true;
        }

        plugin.getCooldownManager().clearPlayer(target);
        sender.sendMessage(plugin.getPrefixo() + " §aCooldown de §e" + target.getName() + " §alimpo.");
        return true;
    }

    private boolean handleToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cComando apenas para jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("myfaq.toggle")) {
            sender.sendMessage("§cSem permissao.");
            return true;
        }

        boolean wasActive = plugin.getCooldownManager().isToggled(player);
        plugin.getCooldownManager().toggle(player);
        boolean nowActive = plugin.getCooldownManager().isToggled(player);

        if (nowActive) {
            sender.sendMessage("§aMyFaq ativado para voce.");
        } else {
            sender.sendMessage("§cMyFaq desativado para voce.");
        }

        return true;
    }
}
