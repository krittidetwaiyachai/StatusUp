package xyz.kaijiieow.statusup.core;

import xyz.kaijiieow.statusup.StatusUp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final StatusUp plugin;
    private FileConfiguration rankupConfig;
    private FileConfiguration starupConfig;
    private FileConfiguration settingsConfig;

    public ConfigManager(StatusUp plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        this.rankupConfig = loadConfig("rankup.yml");
        this.starupConfig = loadConfig("starup.yml");
        this.settingsConfig = loadConfig("settings.yml");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getRankupConfig() {
        return this.rankupConfig;
    }

    public FileConfiguration getStarupConfig() {
        return this.starupConfig;
    }

    public FileConfiguration getSettingsConfig() {
        return this.settingsConfig;
    }
}