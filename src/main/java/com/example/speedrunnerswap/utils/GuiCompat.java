package com.example.speedrunnerswap.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI compatibility helpers for Paper/Spigot 1.21.8 with safe fallbacks.
 */
public final class GuiCompat {
    private GuiCompat() {}

    /** Create an inventory using Component title if available, otherwise String title. */
    public static Inventory createInventory(int size, String title) {
        // Try Bukkit.createInventory(InventoryHolder, int, Component) reflectively
        try {
            Class<?> component = Class.forName("net.kyori.adventure.text.Component");
            java.lang.reflect.Method text = component.getMethod("text", String.class);
            Object comp = text.invoke(null, title);
            java.lang.reflect.Method create = Bukkit.class.getMethod("createInventory", org.bukkit.inventory.InventoryHolder.class, int.class, component);
            return (Inventory) create.invoke(null, null, size, comp);
        } catch (Throwable ignored) {
        }
        // Fallback: String title variant
        return Bukkit.createInventory(null, size, title);
    }

    /** Get the plain title of an InventoryView across API variants. */
    public static String getTitle(InventoryView view) {
        if (view == null) return "";
        // Try InventoryView#title() -> Component
        try {
            java.lang.reflect.Method m = view.getClass().getMethod("title");
            Object comp = m.invoke(view);
            if (comp != null) {
                try {
                    Class<?> serializer = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                    java.lang.reflect.Method plain = serializer.getMethod("plainText");
                    Object inst = plain.invoke(null);
                    java.lang.reflect.Method serialize = inst.getClass().getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
                    return (String) serialize.invoke(inst, comp);
                } catch (Throwable ignored) {
                    return comp.toString();
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback: InventoryView#getTitle()
        try {
            java.lang.reflect.Method m2 = view.getClass().getMethod("getTitle");
            Object s = m2.invoke(view);
            return s != null ? s.toString() : "";
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * Set a display name on an ItemMeta using Component if available, else legacy String.
     */
    public static void setDisplayName(ItemMeta meta, String name) {
        if (meta == null) return;
        // Try ItemMeta#displayName(Component)
        try {
            Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
            java.lang.reflect.Method text = comp.getMethod("text", String.class);
            Object component = text.invoke(null, name);
            java.lang.reflect.Method dm = meta.getClass().getMethod("displayName", comp);
            dm.invoke(meta, component);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback: legacy setDisplayName(String)
        try {
            java.lang.reflect.Method legacy = meta.getClass().getMethod("setDisplayName", String.class);
            legacy.invoke(meta, name);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Set lore on an ItemMeta using Component list if available, else legacy String list.
     */
    public static void setLore(ItemMeta meta, List<String> lines) {
        if (meta == null) return;
        if (lines == null) lines = new ArrayList<>();
        // Try ItemMeta#lore(List<Component>)
        try {
            Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
            List<Object> comps = new ArrayList<>();
            java.lang.reflect.Method text = comp.getMethod("text", String.class);
            for (String s : lines) comps.add(text.invoke(null, s));
            java.lang.reflect.Method lore = meta.getClass().getMethod("lore", List.class);
            lore.invoke(meta, comps);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback: legacy setLore(List<String>)
        try {
            java.lang.reflect.Method legacy = meta.getClass().getMethod("setLore", List.class);
            legacy.invoke(meta, lines);
        } catch (Throwable ignored) {
        }
    }

    /** Extract a plain display name from an ItemMeta across APIs. */
    public static String getDisplayName(ItemMeta meta) {
        if (meta == null) return "";
        // Try displayName() -> Component
        try {
            java.lang.reflect.Method m = meta.getClass().getMethod("displayName");
            Object comp = m.invoke(meta);
            if (comp != null) {
                try {
                    Class<?> serializer = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                    java.lang.reflect.Method plain = serializer.getMethod("plainText");
                    Object inst = plain.invoke(null);
                    java.lang.reflect.Method serialize = inst.getClass().getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
                    return (String) serialize.invoke(inst, comp);
                } catch (Throwable ignored) {
                    return comp.toString();
                }
            }
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Method m2 = meta.getClass().getMethod("getDisplayName");
            Object s = m2.invoke(meta);
            return s != null ? s.toString() : "";
        } catch (Throwable ignored) {}
        return "";
    }
}
