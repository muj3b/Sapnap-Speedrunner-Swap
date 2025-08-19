package com.example.speedrunnerswap.game;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

/**
 * Class to store a player's state for syncing between runners
 */
public class PlayerState {
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final int totalExperience;
    private final float exp;
    private final int level;
    private final int fireTicks;
    private final int remainingAir;
    private final int maximumAir;
    private final GameMode gameMode;
    private final float fallDistance;
    private final boolean allowFlight;
    private final boolean flying;
    private final Collection<PotionEffect> activePotionEffects;
    private final float absorptionAmount;

    public PlayerState(ItemStack[] inventory, ItemStack[] armor, ItemStack offhand, Location location, 
                      double health, int foodLevel, float saturation, float exhaustion, 
                      int totalExperience, float exp, int level, int fireTicks, 
                      int remainingAir, int maximumAir, GameMode gameMode, float fallDistance,
                      boolean allowFlight, boolean flying, Collection<PotionEffect> activePotionEffects,
                      float absorptionAmount) {
        this.inventory = inventory;
        this.armor = armor;
        this.offhand = offhand;
        this.location = location;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.totalExperience = totalExperience;
        this.exp = exp;
        this.level = level;
        this.fireTicks = fireTicks;
        this.remainingAir = remainingAir;
        this.maximumAir = maximumAir;
        this.gameMode = gameMode;
        this.fallDistance = fallDistance;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.activePotionEffects = activePotionEffects;
        this.absorptionAmount = absorptionAmount;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public Location getLocation() {
        return location;
    }

    public double getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getExhaustion() {
        return exhaustion;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public float getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public int getRemainingAir() {
        return remainingAir;
    }

    public int getMaximumAir() {
        return maximumAir;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public float getFallDistance() {
        return fallDistance;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public boolean isFlying() {
        return flying;
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        return activePotionEffects;
    }

    public float getAbsorptionAmount() {
        return absorptionAmount;
    }
}