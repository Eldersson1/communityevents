package com.Eldersson.communityevents;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Communityevents extends JavaPlugin implements Listener {

    private File eventsFile;
    private FileConfiguration eventsConfig;
    private Map<UUID, Location> savedLocations = new ConcurrentHashMap<>();
    private Map<UUID, String> activeEvents = new ConcurrentHashMap<>();
    private Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();
    private Map<UUID, GameMode> savedGameModes = new ConcurrentHashMap<>();
    private List<GameEvent> events = new ArrayList<>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Create and load events file
        createEventsFile();
        loadEvents();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Start the event status checker
        startEventChecker();
        getLogger().info("Running on: " + VersionCompatibility.getServerVersion());
        getLogger().info("Minecraft version: " + VersionCompatibility.getMinecraftVersion());
        getLogger().info("Legacy version: " + VersionCompatibility.isLegacyVersion());

        // Save default config
        saveDefaultConfig();

        // Create and load events file
        createEventsFile();
        loadEvents();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Start the event status checker
        startEventChecker();

        // Try modern command registration first (Paper), then fall back to traditional
        if (!registerCommandsModern()) {
            registerCommandsTraditional();
        }

        try {
            // Access the CommandMap reflectively (for Paper)
            java.lang.reflect.Method getCommandMapMethod = getServer().getClass().getMethod("getCommandMap");
            Object commandMap = getCommandMapMethod.invoke(getServer());

            java.lang.reflect.Method registerMethod = commandMap.getClass().getMethod(
                    "register", String.class, org.bukkit.command.Command.class
            );

            // Register /events
            registerMethod.invoke(commandMap, "communityevents",
                    new PluginCommandWrapper("events", new EventsCommand(this)));

            // Register /event
            registerMethod.invoke(commandMap, "communityevents",
                    new PluginCommandWrapper("event", new EventCommand(this)));

            getLogger().info("Successfully registered /events and /event");
        } catch (Exception e) {
            getLogger().severe("Command registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        saveEvents();
        getLogger().info("CommunityEvents plugin has been disabled!");
    }

    private void createEventsFile() {
        eventsFile = new File(getDataFolder(), "events.yml");
        if (!eventsFile.exists()) {
            eventsFile.getParentFile().mkdirs();
            try {
                eventsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create events.yml: " + e.getMessage());
            }
        }
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);
    }

    private void loadEvents() {
        events.clear();
        if (eventsConfig.contains("events")) {
            for (String key : eventsConfig.getConfigurationSection("events").getKeys(false)) {
                String name = eventsConfig.getString("events." + key + ".name");
                String dateStr = eventsConfig.getString("events." + key + ".date");
                String timeStr = eventsConfig.getString("events." + key + ".time");
                int duration = eventsConfig.getInt("events." + key + ".duration");
                String worldName = eventsConfig.getString("events." + key + ".world");
                double x = eventsConfig.getDouble("events." + key + ".x");
                double y = eventsConfig.getDouble("events." + key + ".y");
                double z = eventsConfig.getDouble("events." + key + ".z");
                float yaw = (float) eventsConfig.getDouble("events." + key + ".yaw");
                float pitch = (float) eventsConfig.getDouble("events." + key + ".pitch");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location location = new Location(world, x, y, z, yaw, pitch);
                    GameEvent event = new GameEvent(name, dateStr, timeStr, duration, location);
                    events.add(event);
                }
            }
        }
    }
    private boolean registerCommandsModern() {
        try {
            // Access the CommandMap reflectively (for Paper)
            java.lang.reflect.Method getCommandMapMethod = getServer().getClass().getMethod("getCommandMap");
            Object commandMap = getCommandMapMethod.invoke(getServer());

            java.lang.reflect.Method registerMethod = commandMap.getClass().getMethod(
                    "register", String.class, org.bukkit.command.Command.class
            );

            // Register /events
            registerMethod.invoke(commandMap, "communityevents",
                    new PluginCommandWrapper("events", new EventsCommand(this)));

            // Register /event
            registerMethod.invoke(commandMap, "communityevents",
                    new PluginCommandWrapper("event", new EventCommand(this)));

            getLogger().info("Successfully registered commands using modern method");
            return true;
        } catch (Exception e) {
            getLogger().info("Modern command registration failed, trying traditional method...");
            return false;
        }
    }
    private void registerCommandsTraditional() {
        try {
            // Traditional command registration (requires commands in plugin.yml)
            org.bukkit.command.PluginCommand eventsCmd = getCommand("events");
            org.bukkit.command.PluginCommand eventCmd = getCommand("event");

            if (eventsCmd != null) {
                eventsCmd.setExecutor(new EventsCommand(this));
                getLogger().info("Registered /events command traditionally");
            }

            if (eventCmd != null) {
                eventCmd.setExecutor(new EventCommand(this));
                eventCmd.setTabCompleter(new EventCommand(this));
                getLogger().info("Registered /event command traditionally");
            }

            if (eventsCmd == null || eventCmd == null) {
                getLogger().warning("Some commands were not registered. Make sure they're defined in plugin.yml");
            }
        } catch (Exception e) {
            getLogger().severe("Traditional command registration also failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveEvents() {
        eventsConfig.set("events", null);
        for (int i = 0; i < events.size(); i++) {
            GameEvent event = events.get(i);
            String path = "events.event" + i;
            eventsConfig.set(path + ".name", event.getName());
            eventsConfig.set(path + ".date", event.getDateString());
            eventsConfig.set(path + ".time", event.getTimeString());
            eventsConfig.set(path + ".duration", event.getDuration());
            eventsConfig.set(path + ".world", event.getLocation().getWorld().getName());
            eventsConfig.set(path + ".x", event.getLocation().getX());
            eventsConfig.set(path + ".y", event.getLocation().getY());
            eventsConfig.set(path + ".z", event.getLocation().getZ());
            eventsConfig.set(path + ".yaw", event.getLocation().getYaw());
            eventsConfig.set(path + ".pitch", event.getLocation().getPitch());
        }
        try {
            eventsConfig.save(eventsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save events.yml: " + e.getMessage());
        }
    }

    // Public methods for command classes
    public void openEventsGUI(Player player) {
        List<GameEvent> upcomingEvents = getUpcomingEvents();

        // Get GUI settings from config
        String title = getConfig().getString("gui.title", "Upcoming Events");
        String titleColor = getConfig().getString("gui.title-color", "&1");
        int configSize = getConfig().getInt("gui.size", 27);

        // Ensure size is valid (multiple of 9, max 54)
        int size = Math.max(9, ((upcomingEvents.size() + 8) / 9) * 9);
        if (configSize > 0 && configSize <= 54 && configSize % 9 == 0) {
            size = Math.max(size, configSize);
        }
        if (size > 54) size = 54;

        // Apply color formatting
        String formattedTitle = ChatColor.translateAlternateColorCodes('&', titleColor + title);
        Inventory gui = Bukkit.createInventory(null, size, formattedTitle);

        for (int i = 0; i < upcomingEvents.size() && i < 54; i++) {
            GameEvent event = upcomingEvents.get(i);
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.GOLD + event.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Date: " + event.getDateString());
            lore.add(ChatColor.YELLOW + "Time: " + event.getTimeString() + " BST");
            lore.add(ChatColor.YELLOW + "Duration: " + event.getDuration() + " minutes");
            lore.add(ChatColor.YELLOW + "Location: " +
                    (int)event.getLocation().getX() + ", " +
                    (int)event.getLocation().getY() + ", " +
                    (int)event.getLocation().getZ());

            if (event.isActive()) {
                lore.add(ChatColor.GREEN + "Status: ACTIVE");
                lore.add(ChatColor.AQUA + "Click to teleport!");
            } else {
                long timeUntil = event.getTimeUntilStart();
                if (timeUntil > 0) {
                    lore.add(ChatColor.YELLOW + "Starts in: " + formatTime(timeUntil));
                    lore.add(ChatColor.GRAY + "Click to teleport (event not started)");
                } else {
                    lore.add(ChatColor.RED + "Status: ENDED");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }

        if (upcomingEvents.isEmpty()) {
            ItemStack noEvents = new ItemStack(Material.BARRIER);
            ItemMeta meta = noEvents.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No upcoming events");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Check back later!"));
            noEvents.setItemMeta(meta);
            gui.setItem(4, noEvents);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        InventoryView view = event.getView();

        // Get title from config for comparison
        String configTitle = getConfig().getString("gui.title", "Upcoming Events");
        String titleColor = getConfig().getString("gui.title-color", "&1");
        String expectedTitle = ChatColor.translateAlternateColorCodes('&', titleColor + configTitle);

        // Check if this is our events GUI
        if (view.getTitle().equals(expectedTitle)) {
            event.setCancelled(true); // Prevent moving/taking items

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            // Handle clicking the item
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();

            if (meta == null || meta.getDisplayName() == null) return;

            String displayName = meta.getDisplayName();

            // Extract event name by removing color codes
            String eventName = ChatColor.stripColor(displayName);

            // Find the event
            GameEvent gameEvent = findEventByName(eventName);
            if (gameEvent != null && clicker instanceof Player) {
                Player player = (Player) clicker;

                if (gameEvent.isActive()) {
                    teleportToEvent(player, gameEvent, true);
                } else if (!gameEvent.hasStarted()) {
                    teleportToEvent(player, gameEvent, false);
                } else {
                    // Event has ended
                    String message = getConfig().getString("messages.event-ended", "&cThis event has already ended!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }

                // Close the GUI
                player.closeInventory();
            }
        }
    }

    private void teleportToEvent(Player player, GameEvent gameEvent, boolean isActive) {
        UUID playerId = player.getUniqueId();

        // Save player's current location
        savedLocations.put(playerId, player.getLocation());

        if (isActive) {
            activeEvents.put(playerId, gameEvent.getName());
        }

        // Save current gamemode if configured
        if (getConfig().getBoolean("events.restore-gamemode-on-return", true)) {
            savedGameModes.put(playerId, player.getGameMode());
        }

        // Clear and save inventory if configured
        if (getConfig().getBoolean("events.clear-inventory-on-teleport", false)) {
            if (getConfig().getBoolean("events.restore-inventory-on-return", true)) {
                savedInventories.put(playerId, player.getInventory().getContents().clone());
            }
            player.getInventory().clear();
        }

        // Set gamemode if configured
        String gameModeStr = getConfig().getString("events.set-gamemode-on-teleport", "none");
        if (!gameModeStr.equalsIgnoreCase("none")) {
            try {
                GameMode gameMode = GameMode.valueOf(gameModeStr.toUpperCase());
                player.setGameMode(gameMode);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid gamemode in config: " + gameModeStr);
            }
        }

        // Remove hunger if configured
        if (getConfig().getBoolean("events.remove-hunger-on-teleport", true)) {
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }

        // Give items if configured
        if (getConfig().getBoolean("events.give-items-on-teleport", false)) {
            List<String> itemList = getConfig().getStringList("give-items-list");
            for (String itemStr : itemList) {
                try {
                    String[] parts = itemStr.split(":");
                    Material material = Material.valueOf(parts[0]);
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    player.getInventory().addItem(new ItemStack(material, amount));
                } catch (Exception e) {
                    getLogger().warning("Invalid item in config: " + itemStr);
                }
            }
        }

        // Teleport to event
        player.teleport(gameEvent.getLocation());

        // Send messages
        if (isActive) {
            String successMsg = getConfig().getString("messages.teleport-success", "&aTeleported to {event}!");
            String infoMsg = getConfig().getString("messages.teleport-back-info", "&eYou will be teleported back when the event ends.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMsg.replace("{event}", gameEvent.getName())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', infoMsg));
        } else {
            String earlyMsg = getConfig().getString("messages.teleport-early", "&eTeleported to {event} (event hasn't started yet)");
            String timeMsg = getConfig().getString("messages.event-starts-in", "&7Event starts in: {time}");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', earlyMsg.replace("{event}", gameEvent.getName())));
            long timeUntil = gameEvent.getTimeUntilStart();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', timeMsg.replace("{time}", formatTime(timeUntil))));
        }
    }

    private GameEvent findEventByName(String name) {
        return events.stream()
                .filter(event -> event.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private List<GameEvent> getUpcomingEvents() {
        List<GameEvent> upcoming = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (GameEvent event : events) {
            // Show events that haven't ended yet
            if (event.getEndTime() > currentTime) {
                upcoming.add(event);
            }
        }

        // Sort by start time
        upcoming.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));
        return upcoming;
    }

    private void startEventChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkEventStatus();
            }
        }.runTaskTimer(this, 0L, 1200L); // Check every minute (1200 ticks)
    }

    private void checkEventStatus() {
        long currentTime = System.currentTimeMillis();
        List<UUID> playersToTeleportBack = new ArrayList<>();

        for (Map.Entry<UUID, String> entry : activeEvents.entrySet()) {
            UUID playerId = entry.getKey();
            String eventName = entry.getValue();
            GameEvent event = findEventByName(eventName);

            if (event != null && !event.isActive() && event.getEndTime() <= currentTime) {
                playersToTeleportBack.add(playerId);
            }
        }

        // Teleport players back
        for (UUID playerId : playersToTeleportBack) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                teleportPlayerBack(player);
            }
            activeEvents.remove(playerId);
        }
    }

    private void teleportPlayerBack(Player player) {
        UUID playerId = player.getUniqueId();

        Location savedLocation = savedLocations.get(playerId);
        if (savedLocation != null) {
            player.teleport(savedLocation);
            savedLocations.remove(playerId);
        }

        // Restore inventory if it was saved
        if (savedInventories.containsKey(playerId)) {
            player.getInventory().setContents(savedInventories.get(playerId));
            savedInventories.remove(playerId);
        }

        // Restore gamemode if it was saved
        if (savedGameModes.containsKey(playerId)) {
            player.setGameMode(savedGameModes.get(playerId));
            savedGameModes.remove(playerId);
        }

        String message = getConfig().getString("messages.teleport-back", "&aEvent ended! You have been teleported back.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    // Public methods for command classes
    public boolean validateDateTime(String date, String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK);
            dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
            dateFormat.parse(date + " " + time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public void addEvent(GameEvent event) {
        events.add(event);
        saveEvents();
    }

    public List<GameEvent> getEvents() {
        return new ArrayList<>(events);
    }
}