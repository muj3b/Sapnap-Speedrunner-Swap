package com.example.speedrunnerswap.tracking;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manager for tracking the active runner
 */
public class TrackerManager {
    
    private final SpeedrunnerSwap plugin;
    private BukkitTask trackerTask;
    
    public TrackerManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start tracking the active runner
     */
    public void startTracking() {
        if (trackerTask != null) {
            trackerTask.cancel();
        }
        
        int updateTicks = plugin.getConfigManager().getTrackerUpdateTicks();
        
        trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            if (activeRunner == null || !activeRunner.isOnline() || !plugin.getGameManager().isGameRunning()) {
                return;
            }
            
            // Update compass for all hunters
            for (Player hunter : plugin.getGameManager().getHunters()) {
                if (hunter.isOnline()) {
                    updateHunterCompass(hunter, activeRunner);
                }
            }
        }, 0L, updateTicks);
    }
    
    /**
     * Stop tracking
     */
    public void stopTracking() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
    }
    
    /**
     * Update a hunter's compass to point to the active runner
     * @param hunter The hunter to update
     * @param target The target to track
     */
    private void updateHunterCompass(Player hunter, Player target) {
        // Check if hunter has a compass
        ItemStack compass = null;
        int slot = -1;
        
        // Check main inventory for compass
        for (int i = 0; i < hunter.getInventory().getSize(); i++) {
            ItemStack item = hunter.getInventory().getItem(i);
            if (item != null && item.getType() == Material.COMPASS) {
                compass = item;
                slot = i;
                break;
            }
        }
        
        // If no compass found, give one
        if (compass == null) {
            compass = new ItemStack(Material.COMPASS);
            slot = hunter.getInventory().firstEmpty();
            if (slot != -1) {
                hunter.getInventory().setItem(slot, compass);
            } else {
                // Inventory full, drop at feet
                hunter.getWorld().dropItem(hunter.getLocation(), compass);
                return;
            }
        }
        
        // Update compass target
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta != null) {
            meta.setLodestoneTracked(false);
            meta.setLodestone(target.getLocation());
            compass.setItemMeta(meta);
            
            // Update the compass in the inventory
            if (slot != -1) {
                hunter.getInventory().setItem(slot, compass);
            }
        }
    }
}