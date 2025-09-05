package com.example.speedrunnerswap.config;

import org.bukkit.Location;
import org.bukkit.World;
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
    private Set<Material> dangerousBlocks;
    
    public ConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Get whether safe swaps are enabled
     * @return True if safe swaps are enabled
     */
    public boolean isSafeSwapEnabled() {
        return config.getBoolean("safe_swap.enabled", false);
    }

    /**
     * Set whether safe swaps are enabled
     * @param enabled True to enable safe swaps
     */
    public void setSafeSwapEnabled(boolean enabled) {
        config.set("safe_swap.enabled", enabled);
        saveConfig();
    }

    /**
     * Get the swap interval in seconds
     * @return The interval in seconds
     */
    public int getSwapInterval() {
        return config.getInt("swap.interval", 60);
    }

    /**
     * Set the swap interval in seconds
     * @param interval The interval in seconds (minimum 30)
     */
    public void setSwapInterval(int interval) {
        config.set("swap.interval", Math.max(30, interval));
        saveConfig();
    }

    /**
     * Get whether randomized swaps are enabled
     * @return True if swaps should be randomized
     */
    public boolean isSwapRandomized() {
        return config.getBoolean("swap.randomize", false);
    }

    /**
     * Backward-compatibility alias for legacy callers
     * @return True if swaps should be randomized
     * @deprecated Use {@link #isSwapRandomized()} instead.
     */
    @Deprecated
    public boolean isRandomizeSwap() {
        return isSwapRandomized();
    }

    /**
     * Set whether swaps should be randomized
     * @param randomized True to enable randomized swaps
     */
    public void setSwapRandomized(boolean randomized) {
        config.set("swap.randomize", randomized);
        saveConfig();
    }
    
    /**
     * Load or reload the configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load runner list
        runnerNames = config.getStringList("teams.runners");
        
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

        // No power-ups in ControlSwap
    }
    
    /**
     * Save the configuration
     */
    public void saveConfig() {
        // Update runner list in config
        config.set("teams.runners", runnerNames);
        
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
        }
    }
    
    /**
     * Remove a player from the runners list
     * @param player The player to remove
     */
    public void removeRunner(Player player) {
        runnerNames.remove(player.getName());
    }

    // Power-ups removed in ControlSwap
    
    // No hunters in ControlSwap
    
    /**
     * Get the list of runner names
     * @return The list of runner names
     */
    public List<String> getRunnerNames() {
        return new ArrayList<>(runnerNames);
    }
    
    // No hunters in ControlSwap

    /**
     * Replace the entire runners name list in memory and persist
     * @param names list of player names
     */
    public void setRunnerNames(java.util.List<String> names) {
        if (names == null) names = java.util.Collections.emptyList();
        this.runnerNames.clear();
        this.runnerNames.addAll(names);
        saveConfig();
    }

    // No hunters in ControlSwap
    
    /**
     * Check if a player is a runner
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runnerNames.contains(player.getName());
    }
    
    // No hunters in ControlSwap
    
    /**
     * Get whether swap randomization is enabled
     * @return True if swap randomization is enabled
     */
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
     * Get the spawn location for players after the game ends.
     * @return The spawn location.
     */
    public org.bukkit.Location getSpawnLocation() {
        double x = config.getDouble("spawn.x", 0);
        double y = config.getDouble("spawn.y", 0);
        double z = config.getDouble("spawn.z", 0);
        String worldName = config.getString("spawn.world", "world");
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0); // Fallback to default world
            plugin.getLogger().warning("Spawn world '" + worldName + "' not found. Using default world: " + world.getName());
        }
        return new org.bukkit.Location(world, x, y, z);
    }





    public void setBroadcastsEnabled(boolean broadcastsEnabled) {
        config.set("broadcasts.enabled", broadcastsEnabled);
        plugin.saveConfig();
    }

    // Voice chat integration removed

    public String getFreezeMode() {
        return config.getString("freeze_mode", "LIMBO");
    }

    public Location getLimboLocation() {
        double x = config.getDouble("limbo.x", 0.5);
        double y = config.getDouble("limbo.y", 200.0);
        double z = config.getDouble("limbo.z", 0.5);
        String worldName = config.getString("limbo.world", "world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0);
            plugin.getLogger().warning("Limbo world '" + worldName + "' not found. Using default world: " + world.getName());
        }
        return new Location(world, x, y, z);
    }

    public void setFreezeMode(String mode) {
        config.set("freeze_mode", mode);
        plugin.saveConfig();
    }

    // Tracking removed

    public String getParticleTrailType() {
        return config.getString("particle_trail.type", "DUST");
    }

    public int[] getParticleTrailColor() {
        List<Integer> rgb = config.getIntegerList("particle_trail.color");
        return new int[]{
            rgb.size() > 0 ? rgb.get(0) : 255,
            rgb.size() > 1 ? rgb.get(1) : 0,
            rgb.size() > 2 ? rgb.get(2) : 0
        };
    }

    public String getGuiMainMenuTitle() {
        return config.getString("gui.main_menu.title", "§6SpeedrunnerSwap - Main Menu");
    }

    public String getGuiTeamSelectorTitle() {
        return config.getString("gui.team_selector.title", "§6SpeedrunnerSwap - Team Selector");
    }

    public String getGuiSettingsTitle() {
        return config.getString("gui.settings.title", "§6SpeedrunnerSwap - Settings");
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
    
    public int getGuiMainMenuRows() {
        // Prefer nested path; fall back to legacy flat key
        return config.getInt("gui.main_menu.rows",
                config.getInt("gui.main_menu_rows", 3));
    }
    
    public int getGuiTeamSelectorRows() {
        // Prefer nested path; fall back to legacy flat key
        return config.getInt("gui.team_selector.rows",
                config.getInt("gui.team_selector_rows", 4));
    }

    public int getGuiSettingsRows() {
        // Prefer nested path; fall back to legacy flat key
        return config.getInt("gui.settings.rows",
                config.getInt("gui.settings_rows", 5));
    }

    public boolean isBroadcastGameEvents() {
        return config.getBoolean("broadcasts.game_events", true);
    }

    public boolean isBroadcastsEnabled() {
        return config.getBoolean("broadcasts.enabled", true);
    }

    public boolean isBroadcastTeamChanges() {
        return config.getBoolean("broadcasts.team_changes", true);
    }

    // Voice chat mute removed

    /**
     * Get whether the freeze mechanic is enabled
     * @return True if enabled
     */
    public boolean isFreezeMechanicEnabled() {
        return config.getBoolean("freeze_mechanic.enabled", false);
    }

    /**
     * Get the freeze duration in ticks
     * @return The duration in ticks
     */
    public int getFreezeDurationTicks() {
        return config.getInt("freeze_mechanic.duration_ticks", 100);
    }

    /**
     * Get the interval to check for freezing in ticks
     * @return The check interval in ticks
     */
    public int getFreezeCheckIntervalTicks() {
        return config.getInt("freeze_mechanic.check_interval_ticks", 10);
    }

    /**
     * Get the maximum distance for freezing
     * @return The max distance
     */
    public double getFreezeMaxDistance() {
        return config.getDouble("freeze_mechanic.max_distance", 50.0);
    }

    /**
     * Get the timer visibility setting for active runners
     * @return The visibility setting ("always", "last_10", or "never")
     */
    public String getRunnerTimerVisibility() {
        return config.getString("timer_visibility.runner_visibility", "last_10");
    }

    /**
     * Get the timer visibility setting for waiting runners
     * @return The visibility setting ("always", "last_10", or "never")
     */
    public String getWaitingTimerVisibility() {
        return config.getString("timer_visibility.waiting_visibility", "always");
    }

    /**
     * Get the timer visibility setting for hunters
     * @return The visibility setting ("always", "last_10", or "never")
     */
    // Hunter visibility and tracker settings removed

    // Power-ups removed

    // Last stand removed

    // Kits and hot potato removed



    /**
     * Set the timer visibility setting for active runners
     * @param visibility The visibility setting ("always", "last_10", or "never")
     */
    public void setRunnerTimerVisibility(String visibility) {
        config.set("timer_visibility.runner_visibility", visibility);
        plugin.saveConfig();
    }

    /**
     * Set the timer visibility setting for waiting runners
     * @param visibility The visibility setting ("always", "last_10", or "never")
     */
    public void setWaitingTimerVisibility(String visibility) {
        config.set("timer_visibility.waiting_visibility", visibility);
        plugin.saveConfig();
    }

    // Hunter timer visibility removed
}
