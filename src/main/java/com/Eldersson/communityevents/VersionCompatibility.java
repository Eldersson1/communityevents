package com.Eldersson.communityevents;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class VersionCompatibility {

    private static final String SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName();
    private static final boolean IS_LEGACY = SERVER_VERSION.contains("1_8") ||
            SERVER_VERSION.contains("1_9") ||
            SERVER_VERSION.contains("1_10") ||
            SERVER_VERSION.contains("1_11") ||
            SERVER_VERSION.contains("1_12");

    /**
     * Get a clock material that works across versions
     */
    public static Material getClockMaterial() {
        try {
            // Try new material name first (1.13+)
            return Material.valueOf("CLOCK");
        } catch (IllegalArgumentException e) {
            try {
                // Fallback to legacy name (1.12 and below)
                return Material.valueOf("WATCH");
            } catch (IllegalArgumentException e2) {
                // Ultimate fallback
                return Material.valueOf("COMPASS");
            }
        }
    }

    /**
     * Get a barrier material that works across versions
     */
    public static Material getBarrierMaterial() {
        try {
            return Material.valueOf("BARRIER");
        } catch (IllegalArgumentException e) {
            // Fallback for very old versions
            return Material.valueOf("BEDROCK");
        }
    }

    /**
     * Get materials that work across versions
     */
    public static Material getMaterial(String modernName, String legacyName) {
        try {
            return Material.valueOf(modernName);
        } catch (IllegalArgumentException e) {
            try {
                return Material.valueOf(legacyName);
            } catch (IllegalArgumentException e2) {
                return Material.valueOf("STONE"); // Ultimate fallback
            }
        }
    }

    /**
     * Create ItemStack with cross-version compatibility
     */
    public static ItemStack createItemStack(String modernMaterial, String legacyMaterial, int amount) {
        Material material = getMaterial(modernMaterial, legacyMaterial);
        return new ItemStack(material, amount);
    }

    /**
     * Check if we're running on a legacy version
     */
    public static boolean isLegacyVersion() {
        return IS_LEGACY;
    }

    /**
     * Get server version info
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Get Minecraft version number
     */
    public static String getMinecraftVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        return bukkitVersion.split("-")[0]; // Gets "1.19.4" from "1.19.4-R0.1-SNAPSHOT"
    }

    /**
     * Check if version is at least the specified version
     */
    public static boolean isVersionAtLeast(String targetVersion) {
        String currentVersion = getMinecraftVersion();
        return compareVersions(currentVersion, targetVersion) >= 0;
    }

    /**
     * Compare two version strings
     * Returns: negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    /**
     * Get appropriate materials for different items
     */
    public static class Materials {
        public static final Material CLOCK = getClockMaterial();
        public static final Material BARRIER = getBarrierMaterial();
        public static final Material DIAMOND_SWORD = getMaterial("DIAMOND_SWORD", "DIAMOND_SWORD");
        public static final Material GOLDEN_APPLE = getMaterial("GOLDEN_APPLE", "GOLDEN_APPLE");
        public static final Material BOW = getMaterial("BOW", "BOW");
        public static final Material ARROW = getMaterial("ARROW", "ARROW");

        // Add more materials as needed for your give-items feature
        public static Material fromString(String materialName) {
            try {
                return Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Try some common legacy conversions
                switch (materialName.toUpperCase()) {
                    case "GRASS_BLOCK": return getMaterial("GRASS_BLOCK", "GRASS");
                    case "OAK_PLANKS": return getMaterial("OAK_PLANKS", "WOOD");
                    case "COBBLESTONE_STAIRS": return getMaterial("COBBLESTONE_STAIRS", "COBBLESTONE_STAIRS");
                    case "OAK_LOG": return getMaterial("OAK_LOG", "LOG");
                    default: return Material.STONE; // Safe fallback
                }
            }
        }
    }
}