package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    
    private final SpeedrunnerSwap plugin;
    
    public GuiManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    public void openMainMenu(Player player) {
        String title = plugin.getConfigManager().getGuiMainMenuTitle();
        int rows = plugin.getConfigManager().getGuiMainMenuRows();
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Team selector button
        ItemStack teamSelector = createItem(Material.PLAYER_HEAD, "§e§lTeam Selector", 
                "§7Click to open the team selection menu");
        inventory.setItem(11, teamSelector);
        
        // Settings button
        ItemStack settings = createItem(Material.COMPARATOR, "§a§lSettings", 
                "§7Click to configure plugin settings");
        inventory.setItem(15, settings);
        
        // Game control buttons
        if (plugin.getGameManager().isGameRunning()) {
            if (plugin.getGameManager().isGamePaused()) {
                ItemStack resume = createItem(Material.LIME_CONCRETE, "§a§lResume Game", 
                        "§7Click to resume the game");
                inventory.setItem(22, resume);
            } else {
                ItemStack pause = createItem(Material.YELLOW_CONCRETE, "§e§lPause Game", 
                        "§7Click to pause the game");
                inventory.setItem(22, pause);
            }
            
            ItemStack stop = createItem(Material.RED_CONCRETE, "§c§lStop Game", 
                    "§7Click to stop the game");
            inventory.setItem(31, stop);
        } else {
            ItemStack start = createItem(Material.GREEN_CONCRETE, "§a§lStart Game", 
                    "§7Click to start the game");
            inventory.setItem(22, start);
        }
        
        // Status button
        ItemStack status = createItem(Material.COMPASS, "§b§lGame Status", 
                getStatusLore());
        inventory.setItem(40, status);
        
        player.openInventory(inventory);
    }
    
    public void openTeamSelector(Player player) {
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        int rows = plugin.getConfigManager().getGuiTeamSelectorRows();
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Back button
        ItemStack back = createItem(Material.ARROW, "§7§lBack", 
                "§7Return to main menu");
        inventory.setItem(0, back);
        
        // Runner team button
        ItemStack runnerTeam = createItem(Material.DIAMOND_BOOTS, "§b§lRunners", 
                "§7Click to select players as runners");
        inventory.setItem(2, runnerTeam);
        
        // Hunter team button
        ItemStack hunterTeam = createItem(Material.IRON_SWORD, "§c§lHunters", 
                "§7Click to select players as hunters");
        inventory.setItem(6, hunterTeam);
        
        // Player heads
        int slot = 18;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= inventory.getSize()) break;
            
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwningPlayer(onlinePlayer);
            meta.setDisplayName("§f" + onlinePlayer.getName());
            
            List<String> lore = new ArrayList<>();
            if (plugin.getGameManager().isRunner(onlinePlayer)) {
                lore.add("§bCurrently a Runner");
            } else if (plugin.getGameManager().isHunter(onlinePlayer)) {
                lore.add("§cCurrently a Hunter");
            } else {
                lore.add("§7Click to assign to a team");
            }
            meta.setLore(lore);
            
            playerHead.setItemMeta(meta);
            inventory.setItem(slot++, playerHead);
        }
        
        player.openInventory(inventory);
    }
    
    public void openSettingsMenu(Player player) {
        String title = plugin.getConfigManager().getGuiSettingsTitle();
        int rows = plugin.getConfigManager().getGuiSettingsRows();
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Back button
        ItemStack back = createItem(Material.ARROW, "§7§lBack", 
                "§7Return to main menu");
        inventory.setItem(0, back);
        
        // Swap interval settings
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        ItemStack swapIntervalType = createItem(
                randomize ? Material.CLOCK : Material.REPEATER,
                "§e§lSwap Type: " + (randomize ? "§aRandom" : "§bFixed"),
                "§7Click to toggle between fixed and random swap intervals");
        inventory.setItem(10, swapIntervalType);
        
        int interval = plugin.getConfigManager().getSwapInterval();
        ItemStack swapInterval = createItem(
                Material.CLOCK,
                "§e§lSwap Interval: §f" + interval + "s",
                "§7Click to change the swap interval");
        inventory.setItem(11, swapInterval);
        
        // Safe swap settings
        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        ItemStack safeSwapToggle = createItem(
                safeSwap ? Material.DIAMOND_BOOTS : Material.LEATHER_BOOTS,
                "§e§lSafe Swap: " + (safeSwap ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle safe swap feature");
        inventory.setItem(13, safeSwapToggle);
        
        // Tracker settings
        boolean trackerEnabled = plugin.getConfigManager().isTrackerEnabled();
        ItemStack trackerToggle = createItem(
                trackerEnabled ? Material.COMPASS : Material.BARRIER,
                "§e§lTracker: " + (trackerEnabled ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle hunter tracking compass");
        inventory.setItem(15, trackerToggle);
        
        boolean showCoords = plugin.getConfigManager().isTrackerShowCoordinates();
        ItemStack coordsToggle = createItem(
                showCoords ? Material.MAP : Material.PAPER,
                "§e§lShow Coordinates: " + (showCoords ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle showing coordinates to hunters");
        inventory.setItem(16, coordsToggle);
        
        // Freeze mode settings
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        ItemStack freezeModeItem = createItem(
                freezeMode.equals("SPECTATOR") ? Material.ENDER_EYE : Material.POTION,
                "§e§lFreeze Mode: §f" + freezeMode,
                "§7Click to toggle between EFFECTS and SPECTATOR mode");
        inventory.setItem(28, freezeModeItem);
        
        // Broadcast settings
        boolean broadcasts = plugin.getConfigManager().isBroadcastsEnabled();
        ItemStack broadcastToggle = createItem(
                broadcasts ? Material.BELL : Material.BARRIER,
                "§e§lBroadcasts: " + (broadcasts ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle game event broadcasts");
        inventory.setItem(31, broadcastToggle);
        
        // Voice chat integration
        boolean voiceChat = plugin.getConfigManager().isVoiceChatIntegrationEnabled();
        ItemStack voiceChatToggle = createItem(
                voiceChat ? Material.NOTE_BLOCK : Material.BARRIER,
                "§e§lVoice Chat Integration: " + (voiceChat ? "§aEnabled" : "§cDisabled"),
                "§7Click to toggle Simple Voice Chat integration");
        inventory.setItem(34, voiceChatToggle);
        
        player.openInventory(inventory);
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private List<String> getStatusLore() {
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Game Running: " + (plugin.getGameManager().isGameRunning() ? "§aYes" : "§cNo"));
        lore.add("§7Game Paused: " + (plugin.getGameManager().isGamePaused() ? "§eYes" : "§aNo"));
        
        if (plugin.getGameManager().isGameRunning()) {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            lore.add("§7Active Runner: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
            lore.add("§7Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            lore.add("");
            lore.add("§bRunners:");
            for (Player runner : plugin.getGameManager().getRunners()) {
                lore.add("§7- §f" + runner.getName());
            }
            
            lore.add("");
            lore.add("§cHunters:");
            for (Player hunter : plugin.getGameManager().getHunters()) {
                lore.add("§7- §f" + hunter.getName());
            }
        }
        
        return lore;
    }
}