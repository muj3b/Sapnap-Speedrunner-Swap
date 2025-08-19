package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class EventListeners implements Listener {
    
    private final SpeedrunnerSwap plugin;
    
    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If the player is a hunter, give them a tracking compass
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isHunter(player)) {
            plugin.getTrackerManager().giveTrackingCompass(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an active runner, pause the game
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() == player &&
            plugin.getConfigManager().isPauseOnDisconnect()) {
            
            plugin.getGameManager().pauseGame();
            plugin.getServer().broadcastMessage("§e[SpeedrunnerSwap] §cGame paused because active runner disconnected.");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, cancel their chat messages
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Only send message to the player
            player.sendMessage("§c[SpeedrunnerSwap] You cannot chat while inactive.");
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, prevent movement
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Only cancel if the player is actually trying to move (not just looking around)
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getY() != event.getTo().getY() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, prevent interaction
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            event.setCancelled(true);
        }
        
        // If the player is a hunter using a compass, update it
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isHunter(player) && 
            plugin.getConfigManager().isTrackerEnabled()) {
            
            ItemStack item = event.getItem();
            if (item != null && item.getType().name().contains("COMPASS")) {
                plugin.getTrackerManager().updateCompass(player);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        
        // Handle main menu clicks
        if (title.equals(plugin.getConfigManager().getGuiMainMenuTitle())) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            
            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD:
                    plugin.getGuiManager().openTeamSelector(player);
                    break;
                case COMPARATOR:
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case GREEN_CONCRETE:
                    plugin.getGameManager().startGame();
                    player.closeInventory();
                    break;
                case YELLOW_CONCRETE:
                    plugin.getGameManager().pauseGame();
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                case LIME_CONCRETE:
                    plugin.getGameManager().resumeGame();
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                case RED_CONCRETE:
                    plugin.getGameManager().stopGame();
                    plugin.getGuiManager().openMainMenu(player);
                    break;
            }
        }
        
        // Handle team selector clicks
        else if (title.equals(plugin.getConfigManager().getGuiTeamSelectorTitle())) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            
            if (event.getCurrentItem().getType() == org.bukkit.Material.ARROW) {
                plugin.getGuiManager().openMainMenu(player);
            }
            else if (event.getCurrentItem().getType() == org.bukkit.Material.PLAYER_HEAD) {
                // Get the player from the head
                String playerName = event.getCurrentItem().getItemMeta().getDisplayName().substring(2); // Remove color code
                Player targetPlayer = plugin.getServer().getPlayerExact(playerName);
                
                if (targetPlayer != null) {
                    // Check which team button was clicked last
                    if (event.getClickedInventory().getItem(2).getType() == org.bukkit.Material.DIAMOND_BOOTS) {
                        // Add to runners
                        plugin.getGameManager().addRunner(targetPlayer);
                    } else {
                        // Add to hunters
                        plugin.getGameManager().addHunter(targetPlayer);
                    }
                    
                    // Refresh the GUI
                    plugin.getGuiManager().openTeamSelector(player);
                }
            }
            else if (event.getCurrentItem().getType() == org.bukkit.Material.DIAMOND_BOOTS) {
                // Mark runner team as selected
                event.getClickedInventory().setItem(2, createGlowingItem(event.getCurrentItem()));
                event.getClickedInventory().setItem(6, createNormalItem(event.getClickedInventory().getItem(6)));
            }
            else if (event.getCurrentItem().getType() == org.bukkit.Material.IRON_SWORD) {
                // Mark hunter team as selected
                event.getClickedInventory().setItem(6, createGlowingItem(event.getCurrentItem()));
                event.getClickedInventory().setItem(2, createNormalItem(event.getClickedInventory().getItem(2)));
            }
        }
        
        // Handle settings menu clicks
        else if (title.equals(plugin.getConfigManager().getGuiSettingsTitle())) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            
            if (event.getCurrentItem().getType() == org.bukkit.Material.ARROW) {
                plugin.getGuiManager().openMainMenu(player);
            }
            // Handle other settings clicks here
            // This would be implemented with more detail in a full version
        }
    }
    
    // Helper method to create a glowing item (simplified for this example)
    private ItemStack createGlowingItem(ItemStack item) {
        // In a real implementation, this would add enchantment glow
        return item;
    }
    
    // Helper method to remove glow from an item
    private ItemStack createNormalItem(ItemStack item) {
        // In a real implementation, this would remove enchantment glow
        return item;
    }
}