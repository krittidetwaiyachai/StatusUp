package xyz.kaijiieow.statusup;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.kaijiieow.statusup.commands.RankupCommand;
import xyz.kaijiieow.statusup.commands.StarupCommand;
import xyz.kaijiieow.statusup.commands.StatusUpAdminCommand;
import xyz.kaijiieow.statusup.commands.StatusUpCommand;
import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.DatabaseManager;
import xyz.kaijiieow.statusup.core.RequirementChecker;
import xyz.kaijiieow.statusup.core.UpgradeService;
import xyz.kaijiieow.statusup.gui.GUIListener;
import xyz.kaijiieow.statusup.gui.GUIManager;
import xyz.kaijiieow.statusup.notifications.DiscordWebhookService;

public final class StatusUp extends JavaPlugin {

    private static StatusUp instance;
    private Economy economy;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RequirementChecker requirementChecker;
    private UpgradeService upgradeService;
    private DiscordWebhookService discordWebhookService;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.requirementChecker = new RequirementChecker(this);
        this.discordWebhookService = new DiscordWebhookService(this);
        this.upgradeService = new UpgradeService(this);
        this.guiManager = new GUIManager(this);

        registerCommands();
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        databaseManager.connect();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    private void registerCommands() {
        getCommand("statusup").setExecutor(new StatusUpCommand(this));
        getCommand("rankup").setExecutor(new RankupCommand(this));
        getCommand("starup").setExecutor(new StarupCommand(this));
        getCommand("st").setExecutor(new StatusUpAdminCommand(this));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static StatusUp getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RequirementChecker getRequirementChecker() {
        return requirementChecker;
    }

    public UpgradeService getUpgradeService() {
        return upgradeService;
    }

    public DiscordWebhookService getDiscordWebhookService() {
        return discordWebhookService;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }
}