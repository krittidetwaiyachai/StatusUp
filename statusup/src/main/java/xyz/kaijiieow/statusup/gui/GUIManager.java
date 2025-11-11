package xyz.kaijiieow.statusup.gui;

import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.UpgradeDetails;
import xyz.kaijiieow.statusup.core.UpgradeService;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
// import org.bukkit.inventory.InventoryHolder; // ไม่ได้ใช้
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    // public static final String TITLE_RANKUP = ChatColor.DARK_AQUA + "Rank Up"; // ลบ
    // public static final String TITLE_STARUP = ChatColor.GOLD + "Star Up"; // ลบ

    public final StatusUp plugin;
    private final UpgradeService upgradeService;
    private final ConfigManager configManager;
    private final FileConfiguration messagesConfig; // เพิ่ม

    public GUIManager(StatusUp plugin, UpgradeService upgradeService, ConfigManager configManager) {
        this.plugin = plugin;
        this.upgradeService = upgradeService;
        this.configManager = configManager;
        this.messagesConfig = configManager.getMessagesConfig(); // เพิ่ม
    }

    private String getMsg(String path) {
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(path, "&cMissing msg: " + path));
    }
    
    private List<String> formatList(List<String> list, Player player, UpgradeDetails details) {
        return list.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .map(line -> PlaceholderAPI.setPlaceholders(player, line))
                .map(line -> line.replace("{current_display}", details.currentGroupDisplay()))
                .map(line -> line.replace("{next_display}", details.nextGroupDisplay()))
                .collect(Collectors.toList());
    }

    public void openRankupGUI(Player player) {
        FileConfiguration config = configManager.getRankupConfig();
        String title = getMsg("gui_titles.rankup"); // แก้
        
        upgradeService.getUpgradeDetails(player, config, "rank-", "ranks")
                .thenAcceptAsync(details -> {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        player.openInventory(createGUI(player, details, title)) // แก้
                    );
                });
    }

    public void openStarupGUI(Player player) {
        FileConfiguration config = configManager.getStarupConfig();
        String title = getMsg("gui_titles.starup"); // แก้

        upgradeService.getUpgradeDetails(player, config, "star-", "stars")
                .thenAcceptAsync(details -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.openInventory(createGUI(player, details, title)) // แก้
                    );
                });
    }

    private Inventory createGUI(Player player, UpgradeDetails details, String title) {
        Inventory inv = Bukkit.createInventory(null, 27, title);

        if (details.isMaxLevel()) {
            inv.setItem(13, createItem(
                Material.BARRIER, 
                getMsg("gui_items.max_level.name"),
                formatList(messagesConfig.getStringList("gui_items.max_level.lore"), player, details)
            ));
            return inv;
        }
        
        boolean canUpgrade = details.canAfford() && details.meetsStats();

        // ===== Info Book =====
        List<String> bookLore = new ArrayList<>();
        List<String> costLines = formatCostsForDisplay(details.costs());
        
        for (String line : messagesConfig.getStringList("gui_items.info_book.lore")) {
            if (line.contains("{costs}")) {
                bookLore.addAll(costLines); // แทรกรายการค่าใช้จ่าย
            } else {
                bookLore.add(line);
            }
        }
        
        inv.setItem(13, createItem(
                Material.BOOK,
                getMsg("gui_items.info_book.name"),
                formatList(bookLore, player, details)
        ));

        // ===== Requirements Paper =====
        List<String> reqLore = new ArrayList<>();
        List<String> requirementLines = formatRequirements(player, details.requirements());
        
        for (String line : messagesConfig.getStringList("gui_items.requirements_paper.lore")) {
            if (line.contains("{requirements}")) {
                reqLore.addAll(requirementLines);
            } else {
                reqLore.add(line);
            }
        }

        inv.setItem(10, createItem(
                Material.PAPER,
                getMsg("gui_items.requirements_paper.name"),
                formatList(reqLore, player, details)
        ));

        // ===== Confirm/Deny Button =====
        if (canUpgrade) {
            inv.setItem(16, createItem(
                    Material.EMERALD_BLOCK,
                    getMsg("gui_items.confirm_button.name"),
                    formatList(messagesConfig.getStringList("gui_items.confirm_button.lore"), player, details)
            ));
        } else {
            String canAffordMsg = details.canAfford() ? getMsg("gui_items.status_lines.can_afford") : getMsg("gui_items.status_lines.cant_afford");
            String meetsStatsMsg = details.meetsStats() ? getMsg("gui_items.status_lines.meets_stats") : getMsg("gui_items.status_lines.doesnt_meet_stats");
            
            List<String> denyLore = messagesConfig.getStringList("gui_items.deny_button.lore").stream()
                    .map(line -> line.replace("{can_afford_msg}", canAffordMsg))
                    .map(line -> line.replace("{meets_stats_msg}", meetsStatsMsg))
                    .collect(Collectors.toList());

            inv.setItem(16, createItem(
                    Material.REDSTONE_BLOCK,
                    getMsg("gui_items.deny_button.name"),
                    formatList(denyLore, player, details)
            ));
        }
        
        return inv;
    }

    // (ย้ายมาจาก UpgradeService)
    private List<String> formatCostsForDisplay(List<String> costs) {
        if (costs == null || costs.isEmpty()) {
            return List.of("&aFree"); // ควรไปอยู่ใน messages.yml แต่ขี้เกียจแก้ละ
        }
        return costs.stream()
                .map(costStr -> {
                    String[] parts = costStr.split(":", 2);
                    if (parts.length != 2) return "&cInvalid Cost";
                    try {
                        String currencyId = parts[0].trim();
                        double amount = Double.parseDouble(parts[1].trim());
                        
                        String currencyName = currencyId.substring(0, 1).toUpperCase() + currencyId.substring(1);
                        if (currencyName.equalsIgnoreCase("vault")) currencyName = "Money";
                        
                        // &7- &e1,000 &7Money
                        return "  &7- " + String.format("&e%,.0f &7%s", amount, currencyName);
                    } catch (Exception e) {
                        return "  &7- &cInvalid Cost Format";
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> formatRequirements(Player player, List<String> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of("&aไม่มีเงื่อนไข"); // ควรไปอยู่ใน messages.yml
        }
        return requirements.stream()
                .map(req -> "  &7- " + PlaceholderAPI.setPlaceholders(player, req))
                .collect(Collectors.toList());
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); // ชื่อนผ่าน getMsg() มาแล้ว
        
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line); // lore ผ่าน formatList() มาแล้ว
        }
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItem(Material material, String name, List<String> loreLines) {
        return createItem(material, name, loreLines.toArray(new String[0]));
    }
}