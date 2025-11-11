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
import su.nightexpress.coinsengine.api.CoinsEngineAPI; // Import CoinsEngine API

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class StatusUp extends JavaPlugin {

    private Economy econ = null;
    private CoinsEngineAPI coinsEngineAPI = null; // Add CoinsEngine API field
    private String economyProvider = "Vault"; // Default provider

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
        this.configManager = new ConfigManager(this);
        configManager.loadConfigs();
        FileConfiguration settings = configManager.getSettingsConfig();

        // Read economy provider setting
        this.economyProvider = settings.getString("economy.provider", "Vault").toLowerCase();

        // Setup chosen economy provider
        if (this.economyProvider.equals("vault")) {
            if (!setupEconomy()) {
                log(Level.SEVERE, "Economy provider set to 'Vault', but no Vault dependency found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            log(Level.INFO, "Using Vault as the economy provider.");
        } else if (this.economyProvider.equals("coinsengine")) {
            if (!setupCoinsEngine()) {
                log(Level.SEVERE, "Economy provider set to 'CoinsEngine', but CoinsEngine plugin not found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            log(Level.INFO, "Using CoinsEngine as the economy provider.");
        } else {
            log(Level.SEVERE, "Invalid economy provider specified in settings.yml: " + this.economyProvider);
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

        this.fileLogger = new FileLogger(this);
        this.databaseManager = new DatabaseManager(this, fileLogger, settings);
        this.discordWebhookService = new DiscordWebhookService(this, fileLogger, settings);
        
        RequirementChecker requirementChecker = new RequirementChecker(this.placeholderApiAvailable);
        
        this.upgradeService = new UpgradeService(
                this, 
                this.econ, // Pass Vault (might be null)
                this.coinsEngineAPI, // Pass CoinsEngine (might be null)
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

    private boolean setupCoinsEngine() {
        if (getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
            return false;
        }
        RegisteredServiceProvider<CoinsEngineAPI> rsp = getServer().getServicesManager().getRegistration(CoinsEngineAPI.class);
        if (rsp == null) {
            return false;
        }
        coinsEngineAPI = rsp.getProvider();
        return coinsEngineAPI != null;
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
    public CoinsEngineAPI getCoinsEngineAPI() { return coinsEngineAPI; }
    public LuckPerms getLuckPermsApi() { return luckPermsApi; }
    public ConfigManager getConfigManager() { return configManager; }
    public FileLogger getFileLogger() { return fileLogger; }
    public UpgradeService getUpgradeService() { return upgradeService; }
    public GUIManager getGUIManager() { return guiManager; }

    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
}