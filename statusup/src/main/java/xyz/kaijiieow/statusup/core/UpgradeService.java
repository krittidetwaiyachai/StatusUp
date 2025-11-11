package xyz.kaijiieow.statusup.core;

import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.notifications.DiscordWebhookService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.economy.Economy;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
// import java.util.stream.Collectors; // ไม่ต้องการตัวนี้แล้ว

public class UpgradeService {

    private final StatusUp plugin;
    private final Economy econ;
    private final CoinsEngineAPI coinsEngineAPI;
    private final LuckPerms lpApi;
    private final RequirementChecker checker;
    
    private final DatabaseManager dbManager;
    private final FileLogger fileLogger;
    private final DiscordWebhookService discordWebhook;

    public UpgradeService(StatusUp plugin, Economy econ, CoinsEngineAPI coinsEngineAPI, LuckPerms lpApi, RequirementChecker checker,
                          DatabaseManager dbManager, FileLogger fileLogger, DiscordWebhookService discordWebhook) {
        this.plugin = plugin;
        this.econ = econ;
        this.coinsEngineAPI = coinsEngineAPI;
        this.lpApi = lpApi;
        this.checker = checker;
        this.dbManager = dbManager;
        this.fileLogger = fileLogger;
        this.discordWebhook = discordWebhook;
    }

    public CompletableFuture<UpgradeDetails> getUpgradeDetails(Player player, FileConfiguration config, String groupPrefix, String configSection) {
        return lpApi.getUserManager().loadUser(player.getUniqueId()).thenApplyAsync(user -> {
            String currentGroup = findCurrentGroup(user, groupPrefix);
            String currentKey = currentGroup.isEmpty() ? "default" : currentGroup;
            String path = configSection + "." + currentKey;

            String nextGroup = config.getString(path + ".next", null);
            boolean isMaxLevel = (nextGroup == null || nextGroup.equalsIgnoreCase("null") || nextGroup.isEmpty());
            
            String currentGroupDisplay = config.getString(path + ".display_name", currentKey);

            if (isMaxLevel) {
                return new UpgradeDetails(currentKey, null, currentGroupDisplay, null, List.of(), null, false, false, true);
            }

            String nextPath = configSection + "." + nextGroup;
            
            List<String> costs = config.getStringList(nextPath + ".costs");
            List<String> requirements = config.getStringList(nextPath + ".requirements");

            boolean canAfford = checkAffordability(player, costs);
            boolean meetsStats = checker.check(player, requirements);
            String nextGroupDisplay = config.getString(nextPath + ".display_name", nextGroup);

            return new UpgradeDetails(
                    currentKey,
                    nextGroup,
                    currentGroupDisplay,
                    nextGroupDisplay,
                    costs,
                    requirements,
                    canAfford,
                    meetsStats,
                    false
            );
        });
    }

    // เปลี่ยน return type
    public CompletableFuture<UpgradeResponse> performUpgrade(Player player, FileConfiguration config, String groupPrefix, String configSection) {
        
        return getUpgradeDetails(player, config, groupPrefix, configSection).thenComposeAsync(details -> {
            if (details.isMaxLevel()) {
                // ส่ง response กลับ
                return CompletableFuture.completedFuture(new UpgradeResponse(UpgradeResult.MAX_LEVEL, details));
            }
            if (!details.canAfford()) {
                fileLogger.logInfo(player.getName() + " failed upgrade (NO_MONEY) to " + details.nextGroup());
                return CompletableFuture.completedFuture(new UpgradeResponse(UpgradeResult.NO_MONEY, details));
            }
            if (!details.meetsStats()) {
                fileLogger.logInfo(player.getName() + " failed upgrade (NO_STATS) to " + details.nextGroup());
                return CompletableFuture.completedFuture(new UpgradeResponse(UpgradeResult.NO_STATS, details));
            }

            if (!withdrawCosts(player, details.costs())) {
                fileLogger.logError("CRITICAL: Failed to withdraw costs even after 'canAfford' check for " + player.getName(), null);
                return CompletableFuture.completedFuture(new UpgradeResponse(UpgradeResult.ERROR, details));
            }


            return lpApi.getUserManager().loadUser(player.getUniqueId()).thenApplyAsync(user -> {
                try {
                    Node newNode = InheritanceNode.builder(details.nextGroup()).build();
                    user.data().add(newNode);

                    if (!details.currentGroup().equals("default")) {
                        Node oldNode = InheritanceNode.builder(details.currentGroup()).build();
                        user.data().remove(oldNode);
                    }
                    lpApi.getUserManager().saveUser(user);

                    String upgradeType = groupPrefix.equals("rank-") ? "RANKUP" : "STARUP";
                    
                    dbManager.logUpgradeAsync(player, details.currentGroup(), details.nextGroup(), upgradeType);
                    
                    fileLogger.logInfo(player.getName() + " upgraded (" + upgradeType + ") from " 
                            + details.currentGroup() + " to " + details.nextGroup());
                    
                    discordWebhook.sendUpgradeNotification(player, details.currentGroupDisplay(), details.nextGroupDisplay(), upgradeType);

                    return new UpgradeResponse(UpgradeResult.SUCCESS, details);
                    
                } catch (Exception e) {
                    fileLogger.logError("Error during LuckPerms update for " + player.getName(), e);
                    return new UpgradeResponse(UpgradeResult.ERROR, details);
                }
            });
        });
    }

    private String findCurrentGroup(User user, String groupPrefix) {
        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .filter(groupName -> groupName.startsWith(groupPrefix))
                .findFirst()
                .orElse("");
    }


    private boolean checkAffordability(Player player, List<String> costs) {
        if (costs == null || costs.isEmpty()) {
            return true;
        }

        for (String costStr : costs) {
            String[] parts = costStr.split(":", 2);
            if (parts.length != 2) {
                plugin.log(Level.WARNING, "Invalid cost format in config: " + costStr);
                continue;
            }

            try {
                String currencyId = parts[0].trim();
                double amount = Double.parseDouble(parts[1].trim());

                if (currencyId.equalsIgnoreCase("vault")) {
                    if (this.econ == null || !this.econ.has(player, amount)) {
                        return false;
                    }
                } else {
                    if (this.coinsEngineAPI == null) {
                        plugin.log(Level.WARNING, "Cost defined for " + currencyId + " but CoinsEngine is not loaded.");
                        return false;
                    }
                    Currency currency = CoinsEngineAPI.getCurrency(currencyId);
                    if (currency == null) {
                        plugin.log(Level.WARNING, "CoinsEngine currency '" + currencyId + "' not found!");
                        return false;
                    }
                    if (CoinsEngineAPI.getBalance(player, currency) < amount) {
                        return false;
                    }
                }
            } catch (NumberFormatException e) {
                plugin.log(Level.WARNING, "Invalid cost amount in config: " + costStr);
                return false;
            }
        }
        return true;
    }

    private boolean withdrawCosts(Player player, List<String> costs) {
        if (costs == null) return true;

        for (String costStr : costs) {
            String[] parts = costStr.split(":", 2);
            if (parts.length != 2) continue;

            try {
                String currencyId = parts[0].trim();
                double amount = Double.parseDouble(parts[1].trim());

                if (currencyId.equalsIgnoreCase("vault")) {
                    if (this.econ != null) {
                        this.econ.withdrawPlayer(player, amount);
                    }
                } else {
                    Currency currency = CoinsEngineAPI.getCurrency(currencyId);
                    if (this.coinsEngineAPI != null && currency != null) {
                        CoinsEngineAPI.removeBalance(player, currency, amount);
                    }
                }
            } catch (NumberFormatException e) {
                plugin.log(Level.WARNING, "Error parsing cost during withdrawal: " + costStr);
            }
        }
        return true;
    }

    // ย้ายเมธอดนี้ไป GUIManager.java
    // public static List<String> formatCostsForDisplay(List<String> costs) { ... }
}