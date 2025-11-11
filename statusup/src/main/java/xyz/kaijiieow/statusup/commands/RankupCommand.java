package xyz.kaijiieow.statusup.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.kaijiieow.statusup.StatusUp;

public class RankupCommand implements CommandExecutor {

    private final StatusUp plugin;

    public RankupCommand(StatusUp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("statusup.rankup")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getGuiManager().openRankupGUI(player);
        return true;
    }
}