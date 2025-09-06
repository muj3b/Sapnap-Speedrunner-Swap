package com.example.speedrunnerswap.utils;

import org.bukkit.entity.Player;

/**
 * Utility for sending action bar messages with compatibility fallbacks.
 *
 * - Primary: Paper/Adventure Player#sendActionBar(Component)
 * - Fallback: Spigot ChatMessageType.ACTION_BAR path via reflection
 */
public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        if (player == null) return;

        // Try Paper/Adventure API first using reflection to avoid hard dependency at runtime
        try {
            Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
            java.lang.reflect.Method text = compClass.getMethod("text", String.class);
            Object component = text.invoke(null, message);
            java.lang.reflect.Method send = player.getClass().getMethod("sendActionBar", compClass);
            send.invoke(player, component);
            return;
        } catch (Throwable ignored) {
        }

        // Fallback: try Spigot action bar via reflection
        try {
            // Player.Spigot spigot = player.spigot();
            java.lang.reflect.Method spigotMethod = player.getClass().getMethod("spigot");
            Object spigot = spigotMethod.invoke(player);

            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBarType = null;
            for (Object c : chatMessageType.getEnumConstants()) {
                if (c.toString().equals("ACTION_BAR")) { actionBarType = c; break; }
            }
            Class<?> baseComponent = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            java.lang.reflect.Method fromLegacy = textComponent.getMethod("fromLegacyText", String.class);
            Object components = fromLegacy.invoke(null, message);
            java.lang.reflect.Method sendMsg = spigot.getClass().getMethod("sendMessage", chatMessageType, java.lang.reflect.Array.newInstance(baseComponent, 0).getClass());
            sendMsg.invoke(spigot, actionBarType, components);
            return;
        } catch (Throwable ignored) {
        }

        // Final fallback: avoid chat spam; only send non-empty messages as chat
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}
