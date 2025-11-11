package xyz.kaijiieow.statusup;

import xyz.kaijiieow.statusup.commands.RankupCommand;
import xyz.kaijiieow.statusup.commands.StarupCommand;
import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.DatabaseManager;
import xyz.kaijiieow.statusup.core.FileLogger;
import xyz.kaijiieow.statusup.core.RequirementChecker;
import xyz.kaijiieow.statusup.core.UpgradeService;
import xyz.kaijiieow.statusup.gui.GUIManager;
import xyz.kaijiieow.statusup.gui.GUIListener;
import xyz.kaijiieow.statusup.notifications.DiscordWebhookService;

import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class StatusUp extends JavaPlugin {

    private Economy econ = null;
    private LuckPerms luckPermsApi = null;
    private boolean placeholderApiAvailable = false;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private FileLogger fileLogger;
    private DiscordWebhookService discordWebhookService;
    private UpgradeService upgradeService;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            log(Level.SEVERE, "Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupLuckPerms()) {
            log(Level.SEVERE, "Disabled due to no LuckPerms dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.placeholderApiAvailable = setupPlaceholderAPI();
        if (!this.placeholderApiAvailable) {
            log(Level.WARNING, "PlaceholderAPI not found. Stat requirements will not work!");
        }

        this.configManager = new ConfigManager(this);
        configManager.loadConfigs();
        FileConfiguration settings = configManager.getSettingsConfig();

        this.fileLogger = new FileLogger(this);
        this.databaseManager = new DatabaseManager(this, fileLogger, settings);
        this.discordWebhookService = new DiscordWebhookService(this, fileLogger, settings);
        
        RequirementChecker requirementChecker = new RequirementChecker(this.placeholderApiAvailable);
        
        this.upgradeService = new UpgradeService(
                this, 
                this.econ, 
                this.luckPermsApi, 
                requirementChecker,
                this.databaseManager,
                this.fileLogger,
                this.discordWebhookService
        );
        
        this.guiManager = new GUIManager(this, this.upgradeService, this.configManager);

        getServer().getPluginManager().registerEvents(new GUIListener(this.guiManager, this.upgradeService), this);

        getCommand("rankup").setExecutor(new RankupCommand(this.guiManager));
        getCommand("starup").setExecutor(new StarupCommand(this.guiManager));

        log(Level.INFO, "StatusUp plugin (v2.0) has been enabled!");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.closeConnection();
        }
        if (this.fileLogger != null) {
            this.fileLogger.close();
        }
        log(Level.INFO, "StatusUp plugin has been disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPermsApi = provider.getProvider();
            return true;
        }
        return false;
    }

    private boolean setupPlaceholderAPI() {
        return (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
    }

    public Economy getEconomy() { return econ; }
    public LuckPerms getLuckPermsApi() { return luckPermsApi; }
    public ConfigManager getConfigManager() { return configManager; }
    public FileLogger getFileLogger() { return fileLogger; }
    public UpgradeService getUpgradeService() { return upgradeService; }
    public GUIManager getGUIManager() { return guiManager; }

    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
}