package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EventListeners implements Listener {
    
    private final SpeedrunnerSwap plugin;
    // No hot-potato logic in ControlSwap
    
    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    // No hunter join/compass or hot-potato mechanics in ControlSwap

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // No special handling on death in ControlSwap
    }

    // No hunter compass management in ControlSwap
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().handlePlayerQuit(player);
        plugin.getGameManager().updateTeams();
    }

    // No hunter compass updates in ControlSwap

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (inventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // If this is one of our control GUIs, let the GUI listener handle it
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof com.example.speedrunnerswap.gui.ControlGuiHolder) {
            return;
        }

        // No GUI or hunter compass management in ControlSwap

        // For non-GUI inventories, enforce runner interaction rules and sync
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isRunner(player)) {
            if (plugin.getGameManager().getActiveRunner() != player) {
                // Inactive runners can't interact
                if (plugin.getConfigManager().isCancelInteractions()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot interact with items while inactive!");
                }
                return;
            } else {
                // Active runner inventory updates
                if (event.getView().getType() != InventoryType.WORKBENCH) {
                    // Schedule sync for next tick to let the current operation complete
                    plugin.getServer().getScheduler().runTask(plugin, () -> syncRunnerInventories(player));
                }
            }
        }
    }
    
    // No plugin GUI in ControlSwap

    // Chat restriction removed for compatibility across server types

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, prevent movement
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Check if getTo() is not null to prevent NullPointerException
            if (event.getTo() != null) {
                // Only cancel if the player is actually trying to move (not just looking around)
                if (event.getFrom().getX() != event.getTo().getX() || 
                    event.getFrom().getY() != event.getTo().getY() || 
                    event.getFrom().getZ() != event.getTo().getZ()) {
                    if (plugin.getConfigManager().isCancelMovement()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Re-apply cages/effects/visibility to avoid queue desync during dimension changes
        plugin.getGameManager().handlePlayerChangedWorld(event.getPlayer());
    }

    /**
     * Synchronize inventories between all runners, with crafting protection
     * @param sourcePlayer The player whose inventory should be copied to others
     */
    private void syncRunnerInventories(Player sourcePlayer) {
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isRunner(sourcePlayer)) return;
        
        // Don't sync while crafting to avoid state corruption
        if (sourcePlayer.getOpenInventory().getType() == InventoryType.WORKBENCH) {
            return;
        }
        
        ItemStack[] contents = sourcePlayer.getInventory().getContents();
        ItemStack[] armor = sourcePlayer.getInventory().getArmorContents();
        ItemStack offhand = sourcePlayer.getInventory().getItemInOffHand();
        
        for (Player runner : plugin.getGameManager().getRunners()) {
            if (runner != sourcePlayer && runner.isOnline() && 
                runner.getOpenInventory().getType() != InventoryType.WORKBENCH) {
                    
                runner.getInventory().setContents(contents.clone());
                runner.getInventory().setArmorContents(armor.clone());
                if (offhand != null) {
                    runner.getInventory().setItemInOffHand(offhand.clone());
                } else {
                    runner.getInventory().setItemInOffHand(null);
                }
                runner.updateInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Update runner list from config names and handle state for joiner
        plugin.getGameManager().updateTeams();

        if (!plugin.getGameManager().isGameRunning()) return;

        Player player = event.getPlayer();
        // Re-apply state/cage/visibility for this player
        plugin.getGameManager().handlePlayerChangedWorld(player);

        // Auto-resume if paused due to disconnects and a runner returned
        if (plugin.getGameManager().isGamePaused() &&
                plugin.getConfigManager().isPauseOnDisconnect() &&
                plugin.getConfigManager().isAutoResumeOnJoin()) {
            boolean anyOnlineRunner = false;
            for (Player r : plugin.getGameManager().getRunners()) {
                if (r.isOnline()) { anyOnlineRunner = true; break; }
            }
            if (anyOnlineRunner) {
                plugin.getGameManager().resumeGame();
            }
        }
    }
}
