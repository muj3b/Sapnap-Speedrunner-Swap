package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ControlGui {
    private final SpeedrunnerSwap plugin;

    public ControlGui(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        int rows = Math.max(1, plugin.getConfigManager().getGuiMainMenuRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiMainMenuTitle();

        Inventory inv = Bukkit.createInventory(null, size, Component.text(title));

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        boolean running = plugin.getGameManager().isGameRunning();
        boolean paused = plugin.getGameManager().isGamePaused();

        // Start/Stop
        if (!running) {
            inv.setItem(10, named(Material.LIME_WOOL, "Start Game", List.of("Begin swapping every "+plugin.getConfigManager().getSwapInterval()+"s")));
        } else {
            inv.setItem(10, named(Material.RED_WOOL, "Stop Game", List.of("End current game")));
        }

        // Pause/Resume
        if (running && !paused) {
            inv.setItem(12, named(Material.YELLOW_WOOL, "Pause", List.of("Temporarily pause swapping")));
        } else if (running && paused) {
            inv.setItem(12, named(Material.ORANGE_WOOL, "Resume", List.of("Resume swapping")));
        } else {
            inv.setItem(12, named(Material.GRAY_WOOL, "Pause", List.of("Game not running")));
        }

        // Shuffle queue
        inv.setItem(14, named(Material.NETHER_STAR, "Shuffle Queue", List.of("Keep active runner, shuffle the rest")));

        // Set runners
        inv.setItem(16, named(Material.BOOK, "Set Runners", List.of("Open the runner selector")));

        // Randomize toggle
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        inv.setItem(22, named(Material.COMPARATOR, randomize ? "Randomize: ON" : "Randomize: OFF",
                List.of("Toggle randomized intervals")));

        // Status
        inv.setItem(24, named(Material.PAPER, "Status", List.of("Show current status in chat")));

        player.openInventory(inv);
    }

    public void openRunnerSelector(Player player) {
        int rows = Math.max(2, plugin.getConfigManager().getGuiTeamSelectorRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        Inventory inv = Bukkit.createInventory(null, size, Component.text(title));

        // Filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Online players as selectable entries
        List<String> selected = plugin.getConfigManager().getRunnerNames();
        int idx = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            try {
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) icon.getItemMeta();
                sm.setOwningPlayer(p);
                sm.displayName(Component.text(p.getName()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                boolean isSel = selected.contains(p.getName());
                lore.add(Component.text(isSel ? "Selected: Yes" : "Selected: No").color(isSel ? NamedTextColor.GREEN : NamedTextColor.RED));
                sm.lore(lore);
                icon.setItemMeta(sm);
            } catch (Throwable t) {
                ItemMeta im = icon.getItemMeta();
                im.displayName(Component.text(p.getName()));
                icon.setItemMeta(im);
            }
            if (idx < size - 9) {
                inv.setItem(idx, icon);
            }
            idx++;
        }

        // Save / Cancel buttons
        inv.setItem(size - 6, named(Material.EMERALD_BLOCK, "Save", List.of("Apply selected runners")));
        inv.setItem(size - 4, named(Material.BARRIER, "Cancel", List.of("Discard changes")));

        player.openInventory(inv);
    }

    private ItemStack named(Material mat, String name, List<String> loreText) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.displayName(Component.text(name).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        if (loreText != null && !loreText.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String s : loreText) lore.add(Component.text(s).color(NamedTextColor.GRAY));
            im.lore(lore);
        }
        it.setItemMeta(im);
        return it;
    }
}

