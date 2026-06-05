package me.zhendersonz.myfaq.listeners;

import me.zhendersonz.myfaq.MyFaq;
import me.zhendersonz.myfaq.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final MyFaq plugin;

    public PlayerListener(MyFaq plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            List<String> welcome = plugin.getFaqManager().getBoasVindas();
            for (String line : welcome) {
                String processed = MessageUtil.applyPlaceholders(line, player, null);
                player.sendMessage(MessageUtil.toComponent(processed));
            }
        }

        boolean opAutoAtivo = plugin.getConfig().getBoolean("op-faq-ativo", true);
        if (opAutoAtivo && player.isOp()) {
            plugin.getCooldownManager().setToggled(player, true);
        }
    }
}
