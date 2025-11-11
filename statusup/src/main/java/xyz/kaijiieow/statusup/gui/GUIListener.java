package xyz.kaijiieow.statusup.gui;

import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.UpgradeResponse; // แก้
import xyz.kaijiieow.statusup.core.UpgradeResult;
import xyz.kaijiieow.statusup.core.UpgradeService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration; // เพิ่ม
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final GUIManager guiManager;
    private final UpgradeService upgradeService;
    private final ConfigManager configManager;
    private final FileConfiguration messagesConfig; // เพิ่ม

    public GUIListener(GUIManager guiManager, UpgradeService upgradeService) {
        this.guiManager = guiManager;
        this.upgradeService = upgradeService;
        this.configManager = guiManager.plugin.getConfigManager();
        this.messagesConfig = configManager.getMessagesConfig(); // เพิ่ม
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // อ่าน title จาก config
        String rankupTitle = format(messagesConfig.getString("gui_titles.rankup"));
        String starupTitle = format(messagesConfig.getString("gui_titles.starup"));
        
        if (title.equals(rankupTitle)) { // แก้
            event.setCancelled(true);
            if (event.getSlot() == 16 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                upgradeService.performUpgrade(player, configManager.getRankupConfig(), "rank-", "ranks")
                        .thenAccept(response -> sendResultMessage(player, response, "ยศ", "prefix.rank")); // แก้
            }
        } 
        else if (title.equals(starupTitle)) { // แก้
            event.setCancelled(true);
            if (event.getSlot() == 16 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                upgradeService.performUpgrade(player, configManager.getStarupConfig(), "star-", "stars")
                        .thenAccept(response -> sendResultMessage(player, response, "ดาว", "prefix.star")); // แก้
            }
        }
    }

    // รื้อใหม่เกือบหมด
    private void sendResultMessage(Player player, UpgradeResponse response, String type, String prefixKey) {
        String prefix = messagesConfig.getString(prefixKey, "");
        String messagePath;

        switch (response.result()) {
            case SUCCESS:
                messagePath = "messages.success";
                break;
            case NO_MONEY:
                messagePath = "messages.no_money";
                break;
            case NO_STATS:
                messagePath = "messages.no_stats";
                break;
            case MAX_LEVEL:
                messagePath = "messages.max_level";
                break;
            case ERROR:
            default:
                messagePath = "messages.error";
                break;
        }

        String message = messagesConfig.getString(messagePath, "&cMessage not found: " + messagePath);
        
        message = message.replace("{prefix}", prefix)
                         .replace("{type}", type)
                         .replace("{from_display}", response.details().currentGroupDisplay())
                         .replace("{to_display}", response.details().nextGroupDisplay() != null ? response.details().nextGroupDisplay() : "MAX");

        player.sendMessage(format(message));
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}