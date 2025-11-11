package xyz.kaijiieow.statusup.gui;

import xyz.kaijiieow.statusup.core.ConfigManager;
import xyz.kaijiieow.statusup.core.UpgradeResult;
import xyz.kaijiieow.statusup.core.UpgradeService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final GUIManager guiManager;
    private final UpgradeService upgradeService;
    private final ConfigManager configManager;

    public GUIListener(GUIManager guiManager, UpgradeService upgradeService) {
        this.guiManager = guiManager;
        this.upgradeService = upgradeService;
        this.configManager = guiManager.plugin.getConfigManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        if (title.equals(GUIManager.TITLE_RANKUP)) {
            event.setCancelled(true);
            if (event.getSlot() == 16 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                upgradeService.performUpgrade(player, configManager.getRankupConfig(), "rank-", "ranks")
                        .thenAccept(result -> sendResultMessage(player, result, "ยศ"));
            }
        } 
        else if (title.equals(GUIManager.TITLE_STARUP)) {
            event.setCancelled(true);
            if (event.getSlot() == 16 && clickedItem.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                upgradeService.performUpgrade(player, configManager.getStarupConfig(), "star-", "stars")
                        .thenAccept(result -> sendResultMessage(player, result, "ดาว"));
            }
        }
    }

    private void sendResultMessage(Player player, UpgradeResult result, String type) {
        String prefix = (type.equals("ดาว") ? "&6[Star] " : "&a[Rank] ");
        switch (result) {
            case SUCCESS:
                player.sendMessage(format(prefix + "&aอัปเกรด " + type + " สำเร็จ!"));
                break;
            case NO_MONEY:
                player.sendMessage(format(prefix + "&cเงินไม่พอ!"));
                break;
            case NO_STATS:
                player.sendMessage(format(prefix + "&cStats ไม่ถึง!"));
                break;
            case MAX_LEVEL:
                player.sendMessage(format(prefix + "&e" + type + " ของคุณสูงสุดแล้ว!"));
                break;
            case ERROR:
            default:
                player.sendMessage(format(prefix + "&cเกิดข้อผิดพลาด โปรดติดต่อแอดมิน"));
                break;
        }
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}