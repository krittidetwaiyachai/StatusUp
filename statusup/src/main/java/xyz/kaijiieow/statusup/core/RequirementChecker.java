package xyz.kaijiieow.statusup.core;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import xyz.kaijiieow.statusup.StatusUp;

import java.util.HashMap;
import java.util.Map;

public class RequirementChecker {

    private final StatusUp plugin;
    private final Economy economy;
    private PlayerPointsAPI playerPointsAPI;

    public RequirementChecker(StatusUp plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            this.playerPointsAPI = PlayerPoints.getInstance().getAPI();
        }
    }

    public Map<String, Boolean> checkRequirements(Player player, ConfigurationSection requirements) {
        Map<String, Boolean> results = new HashMap<>();
        if (requirements == null) return results;

        for (String req : requirements.getKeys(false)) {
            if (req.equalsIgnoreCase("money")) {
                double requiredAmount = requirements.getDouble(req);
                if (economy.getBalance(player) < requiredAmount) {
                    results.put(req, false);
                } else {
                    results.put(req, true);
                }
            } else if (req.equalsIgnoreCase("playerpoints") && playerPointsAPI != null) {
                int requiredAmount = requirements.getInt(req);
                if (playerPointsAPI.look(player.getUniqueId()) < requiredAmount) {
                    results.put(req, false);
                } else {
                    results.put(req, true);
                }
            } else if (req.equalsIgnoreCase("level")) {
                int requiredAmount = requirements.getInt(req);
                if (player.getLevel() < requiredAmount) {
                    results.put(req, false);
                } else {
                    results.put(req, true);
                }
            } else if (req.equalsIgnoreCase("blocks_broken")) {
                int requiredAmount = requirements.getInt(req);
                int playerAmount = player.getStatistic(Statistic.MINE_BLOCK); 
                if (playerAmount < requiredAmount) {
                    results.put(req, false);
                } else {
                    results.put(req, true;
                }
            } else if (req.startsWith("blocks_broken_")) {
                String materialName = req.substring("blocks_broken_".length()).toUpperCase();
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    int requiredAmount = requirements.getInt(req);
                    try {
                        int playerAmount = player.getStatistic(Statistic.MINE_BLOCK, material);
                        if (playerAmount < requiredAmount) {
                            results.put(req, false);
                        } else {
                            results.put(req, true);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid statistic material: " + materialName);
                        results.put(req, false);
                    }
                }
            } else if (req.startsWith("placeholder_")) {
                String placeholder = requirements.getString(req + ".placeholder");
                String condition = requirements.getString(req + ".condition");
                String valueStr = requirements.getString(req + ".value");

                String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, placeholder);
                
                try {
                    double placeholderValue = Double.parseDouble(parsedPlaceholder);
                    double requiredValue = Double.parseDouble(valueStr);

                    boolean met = false;
                    switch (condition.toLowerCase()) {
                        case "==": met = placeholderValue == requiredValue; break;
                        case ">=": met = placeholderValue >= requiredValue; break;
                        case "<=": met = placeholderValue <= requiredValue; break;
                        case ">":  met = placeholderValue > requiredValue;  break;
                        case "<":  met = placeholderValue < requiredValue;  break;
                    }
                    results.put(req, met);

                } catch (NumberFormatException e) {
                    if (condition.equalsIgnoreCase("==")) {
                        results.put(req, parsedPlaceholder.equals(valueStr));
                    } else if (condition.equalsIgnoreCase("!=")) {
                        results.put(req, !parsedPlaceholder.equals(valueStr));
                    } else {
                        plugin.getLogger().warning("Invalid placeholder condition for non-numeric value: " + condition);
                        results.put(req, false);
                    }
                }
            }
        }
        return results;
    }

    public void takeRequirements(Player player, ConfigurationSection requirements) {
        if (requirements == null) return;

        for (String req : requirements.getKeys(false)) {
            if (req.equalsIgnoreCase("money")) {
                economy.withdrawPlayer(player, requirements.getDouble(req));
            } else if (req.equalsIgnoreCase("playerpoints") && playerPointsAPI != null) {
                playerPointsAPI.take(player.getUniqueId(), requirements.getInt(req));
            } else if (req.equalsIgnoreCase("level")) {
                player.setLevel(player.getLevel() - requirements.getInt(req));
            }
        }
    }
}