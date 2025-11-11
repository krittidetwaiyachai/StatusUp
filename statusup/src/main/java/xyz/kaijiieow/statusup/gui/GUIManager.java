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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    public static final String TITLE_RANKUP = ChatColor.DARK_AQUA + "Rank Up";
    public static final String TITLE_STARUP = ChatColor.GOLD + "Star Up";

    public final StatusUp plugin;
    private final UpgradeService upgradeService;
    private final ConfigManager configManager;

    public GUIManager(StatusUp plugin, UpgradeService upgradeService, ConfigManager configManager) {
        this.plugin = plugin;
        this.upgradeService = upgradeService;
        this.configManager = configManager;
    }

    public void openRankupGUI(Player player) {
        FileConfiguration config = configManager.getRankupConfig();
        upgradeService.getUpgradeDetails(player, config, "rank-", "ranks")
                .thenAcceptAsync(details -> {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        player.openInventory(createGUI(player, details, TITLE_RANKUP))
                    );
                });
    }

    public void openStarupGUI(Player player) {
        FileConfiguration config = configManager.getStarupConfig();
        upgradeService.getUpgradeDetails(player, config, "star-", "stars")
                .thenAcceptAsync(details -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.openInventory(createGUI(player, details, TITLE_STARUP))
                    );
                });
    }

    private Inventory createGUI(Player player, UpgradeDetails details, String title) {
        Inventory inv = Bukkit.createInventory(null, 27, title);

        if (details.isMaxLevel()) {
            inv.setItem(13, createItem(Material.BARRIER, "&cคุณถึงระดับสูงสุดแล้ว"));
            return inv;
        }
        
        boolean canUpgrade = details.canAfford() && details.meetsStats();

        inv.setItem(13, createItem(
                Material.BOOK,
                "&bข้อมูลการอัปเกรด",
                "&fยศปัจจุบัน: &7" + details.currentGroupDisplay(),
                "&fยศถัดไป: &a" + details.nextGroupDisplay(),
                "&fค่าใช้จ่าย: &e" + String.format("%,.0f", details.cost())
        ));

        inv.setItem(10, createItem(
                Material.PAPER,
                "&6เงื่อนไข Stats",
                formatRequirements(player, details.requirements())
        ));

        if (canUpgrade) {
            inv.setItem(16, createItem(
                    Material.EMERALD_BLOCK,
                    "&a[!] คลิกเพื่ออัปเกรด",
                    "&7คุณสมบัติของคุณผ่านทั้งหมด"
            ));
        } else {
            inv.setItem(16, createItem(
                    Material.REDSTONE_BLOCK,
                    "&c[!] ยังอัปเกรดไม่ได้",
                    details.canAfford() ? "&a✓ &7เงินพอ" : "&c✗ &7เงินไม่พอ",
                    details.meetsStats() ? "&a✓ &7Stats ถึง" : "&c✗ &7Stats ไม่ถึง"
            ));
        }
        
        return inv;
    }

    private List<String> formatRequirements(Player player, List<String> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of("&aไม่มีเงื่อนไข");
        }
        return requirements.stream()
                .map(req -> "&7- " + PlaceholderAPI.setPlaceholders(player, req))
                .collect(Collectors.toList());
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItem(Material material, String name, List<String> loreLines) {
        return createItem(material, name, loreLines.toArray(new String[0]));
    }
}