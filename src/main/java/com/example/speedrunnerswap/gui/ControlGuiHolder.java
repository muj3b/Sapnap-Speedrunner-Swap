package com.example.speedrunnerswap.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Simple InventoryHolder that marks ControlSwap GUIs with a type so
 * listeners can reliably recognize them across server versions and
 * title/component differences.
 */
public final class ControlGuiHolder implements InventoryHolder {
    public enum Type { MAIN, RUNNER_SELECTOR, ABOUT }

    private final Type type;

    public ControlGuiHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        // Not used; inventories are created externally with this holder.
        return null;
    }
}
