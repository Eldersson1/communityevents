package com.Eldersson.communityevents;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final Communityevents plugin;

    public EventCommand(Communityevents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String message = plugin.getConfig().getString("messages.only-players", "&cOnly players can use this command!");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            return handleCreateCommand(player, args);
        }

        sendUsage(player);
        return true;
    }

    private boolean handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("communityevents.create")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }

        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: /event create <name> <date> <time> <duration in minutes>");
            player.sendMessage(ChatColor.YELLOW + "Example: /event create PvPTournament 26/7/2025 15:00 60");
            return true;
        }

        String name = args[1];
        String date = args[2];
        String time = args[3];
        int duration;

        // Validate duration
        try {
            duration = Integer.parseInt(args[4]);
            if (duration <= 0) {
                player.sendMessage(ChatColor.RED + "Duration must be a positive number!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Duration must be a number (minutes)!");
            return true;
        }

        // Validate date and time format
        if (!plugin.validateDateTime(date, time)) {
            player.sendMessage(ChatColor.RED + "Invalid date or time format!");
            player.sendMessage(ChatColor.YELLOW + "Date format: DD/MM/YYYY (e.g., 26/7/2025)");
            player.sendMessage(ChatColor.YELLOW + "Time format: HH:MM (e.g., 15:00)");
            return true;
        }

        // Check if event name already exists
        for (GameEvent existingEvent : plugin.getEvents()) {
            if (existingEvent.getName().equalsIgnoreCase(name)) {
                player.sendMessage(ChatColor.RED + "An event with that name already exists!");
                return true;
            }
        }

        // Create the event
        Location location = player.getLocation();
        GameEvent event = new GameEvent(name, date, time, duration, location);
        plugin.addEvent(event);

        // Success messages
        player.sendMessage(ChatColor.GREEN + "Event '" + name + "' created successfully!");
        player.sendMessage(ChatColor.YELLOW + "Date: " + date + " at " + time + " BST");
        player.sendMessage(ChatColor.YELLOW + "Duration: " + duration + " minutes");
        player.sendMessage(ChatColor.YELLOW + "Location: " +
                (int)location.getX() + ", " + (int)location.getY() + ", " + (int)location.getZ());
        player.sendMessage(ChatColor.GRAY + "Players can now see this event in /events");

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Community Events ===");
        player.sendMessage(ChatColor.YELLOW + "/events" + ChatColor.WHITE + " - View all upcoming events");
        if (player.hasPermission("communityevents.create")) {
            player.sendMessage(ChatColor.YELLOW + "/event create <name> <date> <time> <duration>");
            player.sendMessage(ChatColor.GRAY + "  Example: /event create PvPTournament 26/7/2025 15:00 60");
            player.sendMessage(ChatColor.GRAY + "  Date format: DD/MM/YYYY | Time format: HH:MM (BST)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("communityevents.create")) {
                completions.add("create");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            completions.add("<EventName>");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.add("DD/MM/YYYY");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.add("HH:MM");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("30", "60", "90", "120"));
        }

        return completions;
    }
}