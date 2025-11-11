package xyz.kaijiieow.statusup.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.kaijiieow.statusup.StatusUp;

public class GUIListener implements Listener {

    private final StatusUp plugin;
    private final GUIManager guiManager;

    private final NamespacedKey guiKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey levelKey;

    public GUIListener(StatusUp plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
        this.guiKey = new NamespacedKey(plugin, "statusup_gui");
        this.actionKey = new NamespacedKey(plugin, "statusup_action");
        this.levelKey = new NamespacedKey(plugin, "statusup_level");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (!data.has(guiKey, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        String guiType = data.get(guiKey, PersistentDataType.STRING);
        String action = data.get(actionKey, PersistentDataType.STRING);

        if (guiType.equals("main")) {
            if ("open_rankup".equals(action)) {
                player.closeInventory();
                guiManager.openRankupGUI(player);
            } else if ("open_starup".equals(action)) {
                player.closeInventory();
                guiManager.openStarupGUI(player);
            }
        } else if (guiType.equals("rankup") || guiType.equals("starup")) {
            if ("open_main".equals(action)) {
                player.closeInventory();
                guiManager.openMainMenu(player);
            } else if (data.has(levelKey, PersistentDataType.INTEGER)) {
                int levelToUpgrade = data.get(levelKey, PersistentDataType.INTEGER);
                
                plugin.getUpgradeService().attemptUpgrade(player, guiType, levelToUpgrade);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (guiType.equals("rankup")) {
                        guiManager.openRankupGUI(player);
                    } else {
                        guiManager.openStarupGUI(player);
                    }
                }, 1L); 
            }
        }
    }
}