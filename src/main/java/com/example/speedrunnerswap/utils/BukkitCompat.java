package com.example.speedrunnerswap.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

/**
 * Small compatibility helpers for API changes across Bukkit/Spigot/Paper versions.
 */
public final class BukkitCompat {
    private BukkitCompat() {}

    /**
     * Resolve the max health attribute across versions.
     *
     * Newer APIs use GENERIC_MAX_HEALTH, older builds used MAX_HEALTH.
     * We resolve by name at runtime to avoid compile-time breakage and
     * provide a safe numeric fallback if the attribute instance is missing.
     */
    public static double getMaxHealthValue(LivingEntity entity) {
        Attribute attr = resolveAttribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (attr != null) {
            AttributeInstance inst = entity.getAttribute(attr);
            if (inst != null) {
                return inst.getValue();
            }
        }
        // Final fallback: assume vanilla default 20.0 hearts if attribute missing
        // (still bounded by current health usages where applied).
        return 20.0D;
    }

    /**
     * Try to resolve an Attribute constant by name without referencing
     * potentially-removed fields directly. Tries names in order.
     */
    public static Attribute resolveAttribute(String... names) {
        for (String name : names) {
            try {
                // Avoid Attribute.valueOf deprecation by using Enum.valueOf
                return java.lang.Enum.valueOf(Attribute.class, name);
            } catch (IllegalArgumentException ignored) {
                // try next
            }
        }
        return null;
    }

    /**
     * Resolve a PotionEffectType by id with common alias handling, compatible with older/newer APIs.
     * Prefers modern keys (e.g., "slowness", "jump_boost"), but maps legacy names as needed.
     */
    public static PotionEffectType resolvePotionEffect(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(java.util.Locale.ROOT).trim();
        // Normalize common legacy aliases to modern names
        key = switch (key) {
            case "increase_damage" -> "strength";
            case "damage_resistance" -> "resistance";
            case "slow" -> "slowness";
            case "jump" -> "jump_boost";
            case "slow_digging" -> "mining_fatigue";
            case "confusion" -> "nausea";
            default -> key;
        };

        // Prefer modern namespaced-key resolver first (non-deprecated)
        try {
            org.bukkit.NamespacedKey ns = org.bukkit.NamespacedKey.minecraft(key);
            PotionEffectType byKey = PotionEffectType.getByKey(ns);
            if (byKey != null) return byKey;
        } catch (Throwable ignored) {
            // continue to legacy fallback
        }

        // Legacy fallback for older servers
        try {
            @SuppressWarnings("deprecation")
            PotionEffectType byName = PotionEffectType.getByName(key.toUpperCase(java.util.Locale.ROOT));
            if (byName != null) return byName;
        } catch (Throwable ignored) {
        }

        return null;
    }

    /**
     * Resolve targeted entity for a player across Paper/Bukkit versions.
     * Tries Player#getTargetEntity(int, boolean), then Player#getTargetEntity(int),
     * finally falls back to a ray trace against entities.
     */
    public static Entity getTargetEntity(Player player, int maxDistance) {
        if (player == null) return null;
        try {
            // Paper 1.19+: getTargetEntity(int maxDistance, boolean ignoreBlocks)
            java.lang.reflect.Method m = Player.class.getMethod("getTargetEntity", int.class, boolean.class);
            Object ent = m.invoke(player, maxDistance, false);
            return (Entity) ent;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            // continue
        }
        try {
            // Older Paper: getTargetEntity(int maxDistance)
            java.lang.reflect.Method m = Player.class.getMethod("getTargetEntity", int.class);
            Object ent = m.invoke(player, maxDistance);
            return (Entity) ent;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
        }

        // Vanilla Bukkit fallback using ray trace
        try {
            Location eye = player.getEyeLocation();
            org.bukkit.util.Vector dir = eye.getDirection();
            org.bukkit.util.RayTraceResult res = player.getWorld().rayTraceEntities(eye, dir, maxDistance, e -> e != player);
            if (res != null) {
                return res.getHitEntity();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
