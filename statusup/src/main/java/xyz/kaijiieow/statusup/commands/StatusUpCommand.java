package xyz.kaijiieow.statusup.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.kaijiieow.statusup.StatusUp;

public class StatusUpCommand implements CommandExecutor {

    private final StatusUp plugin;

    public StatusUpCommand(StatusUp plugin) {
        this.plugin = plugin;
    }

    private String getMsg(String path) {
        FileConfiguration msgConfig = plugin.getConfigManager().getMessagesConfig();
        String msg = msgConfig.getString(path, "&cMessage not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("statusup.admin")) {
                sender.sendMessage(getMsg("commands.no_permission"));
                return true;
            }
            
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(getMsg("commands.reload"));
            return true;
        }

        sender.sendMessage(getMsg("commands.unknown"));
        return true;
    }
}