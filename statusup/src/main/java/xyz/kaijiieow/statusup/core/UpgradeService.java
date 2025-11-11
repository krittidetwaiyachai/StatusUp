package xyz.kaijiieow.statusup.core;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.notifications.DiscordWebhookService;

import java.util.Map;
import java.util.UUID;

public class UpgradeService {

    private final StatusUp plugin;
    private final ConfigManager configManager;
    private final DatabaseManager db;
    private final RequirementChecker requirementChecker;
    private final DiscordWebhookService discordWebhookService;

    public UpgradeService(StatusUp plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.db = plugin.getDatabaseManager();
        this.requirementChecker = plugin.getRequirementChecker();
        this.discordWebhookService = plugin.getDiscordWebhookService();
    }

    public void attemptUpgrade(Player player, String type, int levelToUpgrade) {
        FileConfiguration config = (type.equals("rankup")) ? configManager.getRankupConfig() : configManager.getStarupConfig();
        UUID uuid = player.getUniqueId();
        int currentLevel = (type.equals("rankup")) ? db.getPlayerRank(uuid) : db.getPlayerStar(uuid);

        if (levelToUpgrade <= currentLevel) {
            player.sendMessage(configManager.getMessage(type + "-already-unlocked"));
            return;
        }

        if (levelToUpgrade != currentLevel + 1) {
            player.sendMessage(configManager.getMessage(type + "-must-unlock-previous"));
            return;
        }

        UpgradeDetails details = new UpgradeDetails(type, levelToUpgrade, config);
        if (details.getRequirements() == null) {
            player.sendMessage(ConfigManager.format("&cError: Upgrade level " + levelToUpgrade + " not found in " + type + ".yml"));
            return;
        }

        Map<String, Boolean> requirementsMet = requirementChecker.checkRequirements(player, details.getRequirements());
        boolean allMet = !requirementsMet.containsValue(false);

        if (!allMet) {
            player.sendMessage(configManager.getMessage("requirements-not-met"));
            playSound(player, "upgrade-fail-sound");
            return;
        }

        performUpgrade(player, details);
    }


    private void performUpgrade(Player player, UpgradeDetails details) {
        requirementChecker.takeRequirements(player, details.getRequirements());

        if (details.getType().equals("rankup")) {
            db.setPlayerRank(player.getUniqueId(), details.getLevel());
        } else {
            db.setPlayerStar(player.getUniqueId(), details.getLevel());
        }

        runCommands(player, details.getCommands());
        sendMessages(player, details);
        playSound(player, "upgrade-success-sound");
        discordWebhookService.sendUpgradeNotification(player, details.getType(), details.getDisplayName());
    }

    private void runCommands(Player player, java.util.List<String> commands) {
        if (commands == null) return;
        String playerName = player.getName();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", playerName));
            }
        });
    }

    private void sendMessages(Player player, UpgradeDetails details) {
        String messageKey = details.getType() + "-success";
        String message = configManager.getMessage(messageKey)
                .replace("%" + details.getType() + "%", details.getDisplayName());
        
        player.sendMessage(message);

        if (configManager.getSettings().getBoolean("broadcast." + details.getType())) {
            String broadcastKey = details.getType() + "-broadcast";
            String broadcastMessage = configManager.getMessage(broadcastKey)
                    .replace("%player%", player.getName())
                    .replace("%" + details.getType() + "%", details.getDisplayName());
            
            if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                broadcastMessage = PlaceholderAPI.setPlaceholders(player, broadcastMessage);
            }
            
            Bukkit.broadcastMessage(broadcastMessage);
        }
    }

    private void playSound(Player player, String configKey) {
        if (configManager.getSettings().getBoolean("sounds.enabled")) {
            try {
                String soundName = configManager.getSettings().getString("sounds." + configKey + ".name");
                float volume = (float) configManager.getSettings().getDouble("sounds."D + configKey + ".volume");
                float pitch = (float) configManager.getSettings().getDouble("sounds." + configKey + ".pitch");
                player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound configuration for: " + configKey);
            }
        }
    }
}