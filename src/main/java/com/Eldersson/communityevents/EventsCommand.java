package com.Eldersson.communityevents;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EventsCommand implements CommandExecutor {

    private final Communityevents plugin;

    public EventsCommand(Communityevents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            String message = plugin.getConfig().getString("messages.only-players", "&cOnly players can use this command!");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("communityevents.use")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }

        plugin.openEventsGUI(player);
        return true;
    }
}