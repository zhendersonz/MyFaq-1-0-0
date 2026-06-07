package me.zhendersonz.myfaq.managers;

import me.zhendersonz.myfaq.MyFaq;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final MyFaq plugin;
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerToggle = new ConcurrentHashMap<>();
    private int playerCooldownTime;
    private int globalCooldownTime;
    private File playersFile;

    public CooldownManager(MyFaq plugin) {
        this.plugin = plugin;
        loadData();
        reload();
    }

    public void reload() {
        this.playerCooldownTime = plugin.getConfig().getInt("cooldown-por-jogador", 30);
        this.globalCooldownTime = plugin.getConfig().getInt("cooldown-global", 10);
    }

    private void loadData() {
        playersFile = new File(plugin.getDataFolder(), "jogadores.yml");
        if (!playersFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playersFile);
        if (config.contains("toggles")) {
            for (String uuidStr : config.getConfigurationSection("toggles").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean value = config.getBoolean("toggles." + uuidStr);
                    playerToggle.put(uuid, value);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        if (playersFile == null) return;
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : playerToggle.entrySet()) {
            config.set("toggles." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Erro ao salvar dados de jogadores: " + e.getMessage());
        }
    }

    public boolean canActivate(Player player, String faqId) {
        if (hasPermissionToIgnore(player)) return false;
        if (!isToggled(player)) return false;

        if (playerCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = playerCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
            if (timeLeft > 0) return false;
        }

        String globalKey = "global:" + faqId;
        if (globalCooldowns.containsKey(globalKey)) {
            long timeLeft = globalCooldowns.get(globalKey) - System.currentTimeMillis();
            return timeLeft <= 0;
        }

        return true;
    }

    public void setCooldown(Player player, String faqId) {
        long now = System.currentTimeMillis();
        playerCooldowns.put(player.getUniqueId(),
            now + (playerCooldownTime * 1000L));
        globalCooldowns.put("global:" + faqId,
            now + (globalCooldownTime * 1000L));
    }

    public long getPlayerRemaining(Player player) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) return 0;
        long timeLeft = playerCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, timeLeft / 1000);
    }

    public boolean isToggled(Player player) {
        return playerToggle.getOrDefault(player.getUniqueId(), true);
    }

    public void toggle(Player player) {
        boolean current = playerToggle.getOrDefault(player.getUniqueId(), true);
        playerToggle.put(player.getUniqueId(), !current);
    }

    public void setToggled(Player player, boolean value) {
        playerToggle.put(player.getUniqueId(), value);
    }

    public void clearPlayer(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }

    public int getPlayerCooldownTime() {
        return playerCooldownTime;
    }

    private boolean hasPermissionToIgnore(Player player) {
        return player.hasPermission("myfaq.ignorar");
    }
}
