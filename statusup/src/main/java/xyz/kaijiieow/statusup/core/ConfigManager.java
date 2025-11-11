package xyz.kaijiieow.statusup.core;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.kaijiieow.statusup.StatusUp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {

    private final StatusUp plugin;
    private FileConfiguration settings;
    private FileConfiguration messages;
    private FileConfiguration rankup;
    private FileConfiguration starup;

    private final Map<String, String> messageCache = new HashMap<>();

    public ConfigManager(StatusUp plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        settings = loadConfig("settings.yml");
        messages = loadConfig("messages.yml");
        rankup = loadConfig("rankup.yml");
        starup = loadConfig("starup.yml");

        loadMessages();
    }
    
    public void reloadConfigs() {
        loadConfigs();
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void loadMessages() {
        messageCache.clear();
        if (messages.isConfigurationSection("messages")) {
            for (String key : messages.getConfigurationSection("messages").getKeys(false)) {
                messageCache.put(key, format(messages.getString("messages." + key)));
            }
        }
    }

    public FileConfiguration getSettings() {
        return settings;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getRankupConfig() {
        return rankup;
    }

    public FileConfiguration getStarupConfig() {
        return starup;
    }

    public String getMessage(String key) {
        return messageCache.getOrDefault(key, format("&cMissing message: " + key));
    }

    public static String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> formatList(List<String> list) {
        return list.stream().map(ConfigManager::format).collect(Collectors.toList());
    }
}