package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AboutGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;

    public AboutGuiListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    private boolean isAbout(String title) {
        if (title == null || title.isEmpty()) return false;
        return title.contains(AboutGui.getTitle());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!isAbout(title)) return;

        event.setCancelled(true); // purely informational

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        // Click on the creator head posts the donate link
        String donateUrl = plugin.getConfig().getString("donation.url", "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01");
        Component donate = Component.text("❤ Click to Donate")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Open donation page")))
                .clickEvent(ClickEvent.openUrl(donateUrl));
        player.sendMessage(Component.text("ControlSwap created by muj3b").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(donate);
    }
}

