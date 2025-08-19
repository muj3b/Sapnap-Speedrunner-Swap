package com.example.speedrunnerswap.config;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    
    private final SpeedrunnerSwap plugin;
    private FileConfiguration config;
    private List<String> runnerNames;
    private List<String> hunterNames;
    private Set<Material> dangerousBlocks;
    
    public ConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Load or reload the configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load team lists
        runnerNames = config.getStringList("teams.runners");
        hunterNames = config.getStringList("teams.hunters");
        
        // Load dangerous blocks
        dangerousBlocks = new HashSet<>();
        for (String blockName : config.getStringList("safe_swap.dangerous_blocks")) {
            try {
                Material material = Material.valueOf(blockName);
                dangerousBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in dangerous_blocks: " + blockName);
            }
        }
    }
    
    /**
     * Save the configuration
     */
    public void saveConfig() {
        // Update team lists in config
        config.set("teams.runners", runnerNames);
        config.set("teams.hunters", hunterNames);
        
        plugin.saveConfig();
    }
    
    /**
     * Add a player to the runners list
     * @param player The player to add
     */
    public void addRunner(Player player) {
        String name = player.getName();
        if (!runnerNames.contains(name)) {
            runnerNames.add(name);
            // Remove from hunters if present
            hunterNames.remove(name);
        }
    }
    
    /**
     * Remove a player from the runners list
     * @param player The player to remove
     */
    public void removeRunner(Player player) {
        runnerNames.remove(player.getName());
    }
    
    /**
     * Add a player to the hunters list
     * @param player The player to add
     */
    public void addHunter(Player player) {
        String name = player.getName();
        if (!hunterNames.contains(name)) {
            hunterNames.add(name);
            // Remove from runners if present
            runnerNames.remove(name);
        }
    }
    
    /**
     * Remove a player from the hunters list
     * @param player The player to remove
     */
    public void removeHunter(Player player) {
        hunterNames.remove(player.getName());
    }
    
    /**
     * Get the list of runner names
     * @return The list of runner names
     */
    public List<String> getRunnerNames() {
        return new ArrayList<>(runnerNames);
    }
    
    /**
     * Get the list of hunter names
     * @return The list of hunter names
     */
    public List<String> getHunterNames() {
        return new ArrayList<>(hunterNames);
    }
    
    /**
     * Check if a player is a runner
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runnerNames.contains(player.getName());
    }
    
    /**
     * Check if a player is a hunter
     * @param player The player to check
     * @return True if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunterNames.contains(player.getName());
    }
    
    /**
     * Get whether the swap system should use randomized intervals
     * @return True if randomized intervals should be used
     */
    public boolean isRandomizeSwap() {
        return config.getBoolean("swap.randomize", true);
    }
    
    /**
     * Get the base swap interval in seconds
     * @return The base swap interval
     */
    public int getSwapInterval() {
        return config.getInt("swap.interval", 60);
    }
    
    /**
     * Get the minimum swap interval in seconds
     * @return The minimum swap interval
     */
    public int getMinSwapInterval() {
        return config.getInt("swap.min_interval", 30);
    }
    
    /**
     * Get the maximum swap interval in seconds
     * @return The maximum swap interval
     */
    public int getMaxSwapInterval() {
        return config.getInt("swap.max_interval", 90);
    }
    
    /**
     * Get the jitter standard deviation in seconds
     * @return The jitter standard deviation
     */
    public double getJitterStdDev() {
        return config.getDouble("swap.jitter.stddev", 15);
    }
    
    /**
     * Get whether to clamp jittered intervals within min/max limits
     * @return True if jittered intervals should be clamped
     */
    public boolean isClampJitter() {
        return config.getBoolean("swap.jitter.clamp", true);
    }
    
    /**
     * Get the grace period after swaps in ticks
     * @return The grace period in ticks
     */
    public int getGracePeriodTicks() {
        return config.getInt("swap.grace_period_ticks", 40);
    }
    
    /**
     * Get whether to pause the game when a runner disconnects
     * @return True if the game should pause on disconnect
     */
    public boolean isPauseOnDisconnect() {
        return config.getBoolean("swap.pause_on_disconnect", true);
    }
    
    /**
     * Get whether safe swaps are enabled
     * @return True if safe swaps are enabled
     */
    public boolean isSafeSwapEnabled() {
        return config.getBoolean("safe_swap.enabled", true);
    }
    
    /**
     * Get the horizontal scan radius for safe swaps
     * @return The horizontal scan radius
     */
    public int getSafeSwapHorizontalRadius() {
        return config.getInt("safe_swap.horizontal_radius", 5);
    }
    
    /**
     * Get the vertical scan distance for safe swaps
     * @return The vertical scan distance
     */
    public int getSafeSwapVerticalDistance() {
        return config.getInt("safe_swap.vertical_distance", 10);
    }
    
    /**
     * Get the set of dangerous block materials
     * @return The set of dangerous block materials
     */
    public Set<Material> getDangerousBlocks() {
        return dangerousBlocks;
    }
    
    /**
     * Get the freeze mode for inactive runners
     * @return The freeze mode (EFFECTS or SPECTATOR)
     */
    public String getFreezeMode() {
        return config.getString("freeze_mode", "EFFECTS");
    }
    
    /**
     * Get whether to cancel movement for inactive runners
     * @return True if movement should be canceled
     */
    public boolean isCancelMovement() {
        return config.getBoolean("cancel.movement", true);
    }
    
    /**
     * Get whether to cancel interactions for inactive runners
     * @return True if interactions should be canceled
     */
    public boolean isCancelInteractions() {
        return config.getBoolean("cancel.interactions", true);
    }
    
    /**
     * Get whether the tracker is enabled
     * @return True if the tracker is enabled
     */
    public boolean isTrackerEnabled() {
        return config.getBoolean("tracker.enabled", true);
    }
    
    /**
     * Get how often to update the compass in ticks
     * @return The compass update interval in ticks
     */
    public int getTrackerUpdateTicks() {
        return config.getInt("tracker.update_ticks", 10);
    }
    
    /**
     * Get whether to show coordinates in the action bar for hunters
     * @return True if coordinates should be shown
     */
    public boolean isShowCoordinates() {
        return config.getBoolean("tracker.show_coordinates", true);
    }
    
    /**
     * Get the title for the main menu GUI
     * @return The main menu title
     */
    public String getMainMenuTitle() {
        return config.getString("gui.main_menu.title", "SpeedrunnerSwap Menu");
    }
    
    /**
     * Get the number of rows for the main menu GUI
     * @return The number of rows
     */
    public int getMainMenuRows() {
        return config.getInt("gui.main_menu.rows", 3);
    }
    
    /**
     * Get the title for the team selector GUI
     * @return The team selector title
     */
    public String getTeamSelectorTitle() {
        return config.getString("gui.team_selector.title", "Team Selector");
    }
    
    /**
     * Get the number of rows for the team selector GUI
     * @return The number of rows
     */
    public int getTeamSelectorRows() {
        return config.getInt("gui.team_selector.rows", 4);
    }
    
    /**
     * Get the title for the settings GUI
     * @return The settings title
     */
    public String getSettingsTitle() {
        return config.getString("gui.settings.title", "Settings");
    }
    
    /**
     * Get the number of rows for the settings GUI
     * @return The number of rows
     */
    public int getSettingsRows() {
        return config.getInt("gui.settings.rows", 5);
    }
    
    /**
     * Get whether broadcasts are enabled
     * @return True if broadcasts are enabled
     */
    public boolean isBroadcastsEnabled() {
        return config.getBoolean("broadcasts.enabled", true);
    }
    
    /**
     * Get whether to broadcast game events
     * @return True if game events should be broadcast
     */
    public boolean isBroadcastGameEvents() {
        return config.getBoolean("broadcasts.game_events", true);
    }
    
    /**
     * Get whether to broadcast team changes
     * @return True if team changes should be broadcast
     */
    public boolean isBroadcastTeamChanges() {
        return config.getBoolean("broadcasts.team_changes", true);
    }
    
    /**
     * Get whether voice chat integration is enabled
     * @return True if voice chat integration is enabled
     */
    public boolean isVoiceChatEnabled() {
        return config.getBoolean("voice_chat.enabled", true);
    }
    
    /**
     * Get whether to mute inactive runners
     * @return True if inactive runners should be muted
     */
    public boolean isMuteInactiveRunners() {
        return config.getBoolean("voice_chat.mute_inactive_runners", true);
    }
}