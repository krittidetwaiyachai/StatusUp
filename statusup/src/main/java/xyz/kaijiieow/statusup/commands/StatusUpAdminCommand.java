package xyz.kaijiieow.statusup.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.core.ConfigManager;

public class StatusUpAdminCommand implements CommandExecutor {

    private final StatusUp plugin;
    private final ConfigManager configManager;

    public StatusUpAdminCommand(StatusUp plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("statusup.admin")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }

            configManager.reloadConfigs();
            sender.sendMessage(configManager.getMessage("plugin-reloaded"));
            return true;
        }
        
        sender.sendMessage(ConfigManager.format("&cUsage: /st reload"));
        return true;
    }
}