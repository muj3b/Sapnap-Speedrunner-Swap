package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class AboutGui {
    private final SpeedrunnerSwap plugin;
    private static final String TITLE = "ControlSwap — About";

    public AboutGui(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void openFor(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(TITLE));

        // Filler panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Creator head in the top-right corner
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            OfflinePlayer creator = Bukkit.getOfflinePlayer("muj3b");
            meta.setOwningPlayer(creator);
        } catch (Throwable ignored) {}
        meta.displayName(Component.text("Creator: muj3b"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to open donation link"));
        meta.lore(lore);
        head.setItemMeta(meta);
        inv.setItem(8, head);

        player.openInventory(inv);
    }

    public static String getTitle() {
        return TITLE;
    }
}

