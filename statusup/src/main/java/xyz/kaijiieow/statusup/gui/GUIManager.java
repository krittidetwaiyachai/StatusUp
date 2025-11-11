package xyz.kaijiieow.statusup.gui;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.DatabaseManager;
import xyz.kaijiieow.statusup.core.RequirementChecker;
import xyz.kaijiieow.statusup.core.UpgradeDetails;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GUIManager {

    private final StatusUp plugin;
    private final ConfigManager configManager;
    private final DatabaseManager db;
    private final RequirementChecker requirementChecker;

    private final NamespacedKey guiKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey levelKey;
    
    private boolean isPapiEnabled;

    public GUIManager(StatusUp plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.db = plugin.getDatabaseManager();
        this.requirementChecker = plugin.getRequirementChecker();
        
        this.guiKey = new NamespacedKey(plugin, "statusup_gui");
        this.actionKey = new NamespacedKey(plugin, "statusup_action");
        this.levelKey = new NamespacedKey(plugin, "statusup_level");
        
        this.isPapiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void openMainMenu(Player player) {
        FileConfiguration guiConfig = configManager.getSettings().getConfigurationSection("main-gui");
        String title = ConfigManager.format(guiConfig.getString("title", "&8StatusUP Main Menu"));
        int size = guiConfig.getInt("size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection rankupItemSection = guiConfig.getConfigurationSection("rankup-item");
        inv.setItem(rankupItemSection.getInt("slot", 12), 
            createGuiItem(
                Material.matchMaterial(rankupItemSection.getString("material", "NETHER_STAR")),
                rankupItemSection.getString("name", "&aRankUP"),
                rankupItemSection.getStringList("lore"),
                "main",
                "open_rankup"
            )
        );

        ConfigurationSection starupItemSection = guiConfig.getConfigurationSection("starup-item");
        inv.setItem(starupItemSection.getInt("slot", 14), 
            createGuiItem(
                Material.matchMaterial(starupItemSection.getString("material", "BEACON")),
                starupItemSection.getString("name", "&bStarUP"),
                starupItemSection.getStringList("lore"),
                "main",
                "open_starup"
            )
        );

        fillGlass(inv, guiConfig.getConfigurationSection("fill-glass"));
        
        player.openInventory(inv);
    }

    public void openRankupGUI(Player player) {
        openUpgradeGUI(player, "rankup", configManager.getRankupConfig());
    }

    public void openStarupGUI(Player player) {
        openUpgradeGUI(player, "starup", configManager.getStarupConfig());
    }

    private void openUpgradeGUI(Player player, String type, FileConfiguration config) {
        FileConfiguration guiConfig = config.getConfigurationSection("gui");
        String title = ConfigManager.format(guiConfig.getString("title", "&8Upgrade Menu"));
        int size = guiConfig.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        int currentLevel = (type.equals("rankup")) ? db.getPlayerRank(player.getUniqueId()) : db.getPlayerStar(player.getUniqueId());

        ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");
        if (upgradesSection == null) {
            player.sendMessage(ConfigManager.format("&cError: 'upgrades' section not found in " + type + ".yml"));
            return;
        }

        List<String> sortedKeys = new ArrayList<>(upgradesSection.getKeys(false));
        try {
            sortedKeys.sort(Comparator.comparingInt(Integer::parseInt));
        } catch (NumberFormatException e) {
            plugin.getLogger().severe("Error sorting upgrade keys in " + type + ".yml. Ensure keys are numbers.");
            return;
        }

        for (String key : sortedKeys) {
            String path = "upgrades." + key;
            int rankLevel = Integer.parseInt(key);
            
            boolean isUnlocked = rankLevel <= currentLevel;
            boolean canUpgrade = rankLevel == currentLevel + 1;
            boolean isLocked = rankLevel > currentLevel + 1;

            ConfigurationSection itemSection = config.getConfigurationSection(path + ".item");
            if (itemSection == null) continue;

            String materialName;
            String itemName;
            List<String> itemLore;
            boolean addGlow = false;

            if (isUnlocked) {
                materialName = itemSection.getString("unlocked-material", itemSection.getString("material", "EMERALD_BLOCK"));
                itemName = itemSection.getString("unlocked-name", itemSection.getString("name"));
                itemLore = itemSection.getStringList("unlocked-lore");
                addGlow = itemSection.getBoolean("unlocked-glow", true);
            } else if (canUpgrade) {
                materialName = itemSection.getString("can-upgrade-material", itemSection.getString("material", "GOLD_BLOCK"));
                itemName = itemSection.getString("can-upgrade-name", itemSection.getString("name"));
                itemLore = itemSection.getStringList("can-upgrade-lore");
                addGlow = itemSection.getBoolean("can-upgrade-glow", true);
            } else { // isLocked
                materialName = itemSection.getString("locked-material", "BARRIER");
                itemName = itemSection.getString("locked-name", itemSection.getString("name"));
                itemLore = itemSection.getStringList("locked-lore");
                addGlow = itemSection.getBoolean("locked-glow", false);
            }
            
            ItemStack item = new ItemStack(Material.matchMaterial(materialName.toUpperCase()), 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ConfigManager.format(itemName));

            List<String> finalLore = new ArrayList<>();
            for (String line : itemLore) {
                finalLore.add(ConfigManager.format(line));
            }

            UpgradeDetails details = new UpgradeDetails(type, rankLevel, config);
            Map<String, Boolean> requirementsMet = requirementChecker.checkRequirements(player, details.getRequirements());
            boolean allRequirementsMet = !requirementsMet.containsValue(false);

            List<String> reqDisplayLore = getRequirementLore(player, details.getRequirements(), requirementsMet);
            finalLore.addAll(reqDisplayLore);
            
            finalLore.add("");
            if (isUnlocked) {
                finalLore.add(ConfigManager.format(itemSection.getString("status-unlocked", "&a&lUNLOCKED")));
            } else if (isLocked) {
                finalLore.add(ConfigManager.format(itemSection.getString("status-locked", "&c&lLOCKED")));
            } else if (canUpgrade) {
                if (allRequirementsMet) {
                    finalLore.add(ConfigManager.format(itemSection.getString("status-can-upgrade", "&a&lCLICK TO UPGRADE")));
                } else {
                    finalLore.add(ConfigManager.format(itemSection.getString("status-requirements-not-met", "&c&lREQUIREMENTS NOT MET")));
                }
            }

            if (isPapiEnabled) {
                finalLore = finalLore.stream()
                                     .map(line -> PlaceholderAPI.setPlaceholders(player, line))
                                     .collect(Collectors.toList());
            }

            meta.setLore(finalLore);
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, rankLevel);
            
            if (addGlow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
            inv.setItem(itemSection.getInt("slot"), item);
        }
        
        ConfigurationSection backButtonSection = guiConfig.getConfigurationSection("back-button");
        if (backButtonSection != null && backButtonSection.getBoolean("enabled", true)) {
            inv.setItem(backButtonSection.getInt("slot", 49),
                createGuiItem(
                    Material.matchMaterial(backButtonSection.getString("material", "ARROW")),
                    backButtonSection.getString("name", "&cBack"),
                    backButtonSection.getStringList("lore"),
                    type, 
                    "open_main" 
                )
            );
        }

        fillGlass(inv, guiConfig.getConfigurationSection("fill-glass"));

        player.openInventory(inv);
    }

    private List<String> getRequirementLore(Player player, ConfigurationSection requirements, Map<String, Boolean> requirementsMet) {
        List<String> lore = new ArrayList<>();
        if (requirements == null || requirements.getKeys(false).isEmpty()) {
            return lore;
        }

        String header = configManager.getMessages().getString("gui-lore-headers.requirements", "&eRequirements:");
        lore.add(ConfigManager.format(header));

        String metFormat = ConfigManager.format(configManager.getMessages().getString("gui-lore-formats.requirement-met", "&a✔ &7{name}: &f{current}/{required}"));
        String notMetFormat = ConfigManager.format(configManager.getMessages().getString("gui-lore-formats.requirement-not-met", "&c✘ &7{name}: &f{current}/{required}"));
        String metFormatNoProgress = ConfigManager.format(configManager.getMessages().getString("gui-lore-formats.requirement-met-no-progress", "&a✔ &7{name}"));
        String notMetFormatNoProgress = ConfigManager.format(configManager.getMessages().getString("gui-lore-formats.requirement-not-met-no-progress", "&c✘ &7{name}"));


        for (String reqKey : requirements.getKeys(false)) {
            boolean met = requirementsMet.getOrDefault(reqKey, false);
            String display = ConfigManager.format("&7" + reqKey);
            String format = met ? metFormat : notMetFormat;
            String formatNoProgress = met ? metFormatNoProgress : notMetFormatNoProgress;

            String current = "0";
            String required = "0";

            if (reqKey.equalsIgnoreCase("money")) {
                display = configManager.getMessages().getString("gui-lore-names.money", "Money");
                current = String.format("%,.0f", plugin.getEconomy().getBalance(player));
                required = String.format("%,.0f", requirements.getDouble(reqKey));
            } else if (reqKey.equalsIgnoreCase("level")) {
                display = configManager.getMessages().getString("gui-lore-names.level", "XP Level");
                current = String.valueOf(player.getLevel());
                required = requirements.getString(reqKey);
            } else if (reqKey.equalsIgnoreCase("playerpoints")) {
                display = configManager.getMessages().getString("gui-lore-names.playerpoints", "Player Points");
                try {
                    current = String.valueOf(Bukkit.getPluginManager().isPluginEnabled("PlayerPoints") ? PlayerPoints.getInstance().getAPI().look(player.getUniqueId()) : 0);
                } catch (Exception e) { current = "0"; }
                required = requirements.getString(reqKey);
            } else if (reqKey.equalsIgnoreCase("blocks_broken")) {
                display = configManager.getMessages().getString("gui-lore-names.blocks_broken", "Blocks Broken");
                current = String.valueOf(player.getStatistic(Statistic.MINE_BLOCK));
                required = requirements.getString(reqKey);
            } else if (reqKey.startsWith("blocks_broken_")) {
                String matName = reqKey.substring("blocks_broken_".length());
                display = configManager.getMessages().getString("gui-lore-names.blocks_broken_specific", "Break {material}")
                    .replace("{material}", matName);
                try {
                    current = String.valueOf(player.getStatistic(Statistic.MINE_BLOCK, Material.matchMaterial(matName.toUpperCase())));
                } catch (Exception e) { current = "0"; }
                required = requirements.getString(reqKey);
            } else if (reqKey.startsWith("placeholder_")) {
                display = requirements.getString(reqKey + ".display", "Custom Requirement");
                String placeholder = requirements.getString(reqKey + ".placeholder");
                current = isPapiEnabled ? PlaceholderAPI.setPlaceholders(player, placeholder) : "PAPI needed";
                required = requirements.getString(reqKey + ".value");
                
                String condition = requirements.getString(reqKey + ".condition", "==");
                if (condition.equals("==") || condition.equals("!=")) {
                    format = formatNoProgress.replace("{name}", display);
                    lore.add(format);
                    continue;
                }
            } else {
                 lore.add(formatNoProgress.replace("{name}", display));
                 continue;
            }
            
            lore.add(format.replace("{name}", display)
                           .replace("{current}", current)
                           .replace("{required}", required));
        }

        return lore;
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore, String guiType, String action) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ConfigManager.format(name));
        meta.setLore(ConfigManager.formatList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, guiType);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);

        item.setItemMeta(meta);
        return item;
    }
    
    private void fillGlass(Inventory inv, ConfigurationSection glassConfig) {
        if (glassConfig == null || !glassConfig.getBoolean("enabled", false)) {
            return;
        }
        
        ItemStack glass = new ItemStack(Material.matchMaterial(glassConfig.getString("material", "BLACK_STAINED_GLASS_PANE")), 1);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(ConfigManager.format(glassConfig.getString("name", " ")));
        meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "filler");
        glass.setItemMeta(meta);
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}