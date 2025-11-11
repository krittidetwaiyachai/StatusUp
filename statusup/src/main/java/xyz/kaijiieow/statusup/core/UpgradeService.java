package xyz.kaijiieow.statusup.core;

import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.notifications.DiscordWebhookService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UpgradeService {

    private final StatusUp plugin;
    private final Economy econ;
    private final LuckPerms lpApi;
    private final RequirementChecker checker;
    
    private final DatabaseManager dbManager;
    private final FileLogger fileLogger;
    private final DiscordWebhookService discordWebhook;

    public UpgradeService(StatusUp plugin, Economy econ, LuckPerms lpApi, RequirementChecker checker,
                          DatabaseManager dbManager, FileLogger fileLogger, DiscordWebhookService discordWebhook) {
        this.plugin = plugin;
        this.econ = econ;
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
                return new UpgradeDetails(currentKey, null, currentGroupDisplay, null, 0, null, false, false, true);
            }

            String nextPath = configSection + "." + nextGroup;
            double cost = config.getDouble(nextPath + ".cost");
            List<String> requirements = config.getStringList(nextPath + ".requirements");

            boolean canAfford = econ.getBalance(player) >= cost;
            boolean meetsStats = checker.check(player, requirements);
            String nextGroupDisplay = config.getString(nextPath + ".display_name", nextGroup);

            return new UpgradeDetails(
                    currentKey,
                    nextGroup,
                    currentGroupDisplay,
                    nextGroupDisplay,
                    cost,
                    requirements,
                    canAfford,
                    meetsStats,
                    false
            );
        });
    }

    public CompletableFuture<UpgradeResult> performUpgrade(Player player, FileConfiguration config, String groupPrefix, String configSection) {
        
        return getUpgradeDetails(player, config, groupPrefix, configSection).thenComposeAsync(details -> {
            if (details.isMaxLevel()) {
                return CompletableFuture.completedFuture(UpgradeResult.MAX_LEVEL);
            }
            if (!details.canAfford()) {
                fileLogger.logInfo(player.getName() + " failed upgrade (NO_MONEY) to " + details.nextGroup());
                return CompletableFuture.completedFuture(UpgradeResult.NO_MONEY);
            }
            if (!details.meetsStats()) {
                fileLogger.logInfo(player.getName() + " failed upgrade (NO_STATS) to " + details.nextGroup());
                return CompletableFuture.completedFuture(UpgradeResult.NO_STATS);
            }

            econ.withdrawPlayer(player, details.cost());

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

                    return UpgradeResult.SUCCESS;
                    
                } catch (Exception e) {
                    fileLogger.logError("Error during LuckPerms update for " + player.getName(), e);
                    return UpgradeResult.ERROR;
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
}