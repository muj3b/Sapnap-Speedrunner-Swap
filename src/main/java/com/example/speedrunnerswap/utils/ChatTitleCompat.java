package com.example.speedrunnerswap.utils;

import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Title and chat compatibility helpers for Paper/Spigot with fallbacks.
 */
public final class ChatTitleCompat {
    private ChatTitleCompat() {}

    /** Show a title using Adventure API if present, else Player#sendTitle. Durations in milliseconds. */
    public static void showTitle(Player player, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        if (player == null) return;
        // Try Player#showTitle(Title) with Adventure types
        try {
            Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
            Class<?> titleCls = Class.forName("net.kyori.adventure.title.Title");
            Class<?> timesCls = Class.forName("net.kyori.adventure.title.Title$Times");
            java.lang.reflect.Method text = comp.getMethod("text", String.class);
            Object titleComp = text.invoke(null, title != null ? title : "");
            Object subComp = text.invoke(null, subtitle != null ? subtitle : "");
            java.lang.reflect.Method times = timesCls.getMethod("times", Duration.class, Duration.class, Duration.class);
            Object timesObj = times.invoke(null, Duration.ofMillis(fadeInMs), Duration.ofMillis(stayMs), Duration.ofMillis(fadeOutMs));
            java.lang.reflect.Method makeTitle = titleCls.getMethod("title", comp, comp, timesCls);
            Object t = makeTitle.invoke(null, titleComp, subComp, timesObj);
            java.lang.reflect.Method show = player.getClass().getMethod("showTitle", titleCls);
            show.invoke(player, t);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback: legacy sendTitle(String, String, int, int, int) via reflection (avoid deprecation warnings)
        try {
            int fi = (int) Math.max(0, fadeInMs / 50);
            int st = (int) Math.max(0, stayMs / 50);
            int fo = (int) Math.max(0, fadeOutMs / 50);
            java.lang.reflect.Method legacy = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            legacy.invoke(player, title != null ? title : "", subtitle != null ? subtitle : "", fi, st, fo);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Send a chat line. Use legacy String path intentionally to avoid
     * client-side chat log serialization errors seen on some 1.21.x builds
     * when sending Adventure Components from plugins.
     */
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(message);
    }

    /** Send a clickable URL using the Spigot/Bungee chat component path (no Adventure). */
    public static void sendClickableUrl(Player player, String label, String url) {
        if (player == null || url == null) return;
        try {
            // player.spigot().sendMessage(BaseComponent...)
            java.lang.reflect.Method spigotMethod = player.getClass().getMethod("spigot");
            Object spigot = spigotMethod.invoke(player);

            Class<?> baseComp = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class<?> clickEvent = Class.forName("net.md_5.bungee.api.chat.ClickEvent");
            Class<?> clickAction = Class.forName("net.md_5.bungee.api.chat.ClickEvent$Action");

            // Build components
            Object prefix = textComp.getConstructor(String.class).newInstance(label != null ? label : "");
            Object link = textComp.getConstructor(String.class).newInstance(url);

            Object OPEN_URL = null;
            for (Object e : clickAction.getEnumConstants()) {
                if ("OPEN_URL".equals(e.toString())) { OPEN_URL = e; break; }
            }
            Object ce = clickEvent.getConstructor(clickAction, String.class).newInstance(OPEN_URL, url);
            link.getClass().getMethod("setClickEvent", clickEvent).invoke(link, ce);
            // Optional: underline link for visibility if available
            try {
                link.getClass().getMethod("setUnderlined", boolean.class).invoke(link, true);
            } catch (Throwable ignored) {}

            Object array = java.lang.reflect.Array.newInstance(baseComp, 2);
            java.lang.reflect.Array.set(array, 0, prefix);
            java.lang.reflect.Array.set(array, 1, link);

            java.lang.reflect.Method send = spigot.getClass().getMethod("sendMessage", array.getClass());
            send.invoke(spigot, array);
        } catch (Throwable t) {
            // Fallback: plain message with raw URL (still clickable on most clients)
            player.sendMessage((label != null ? label : "") + url);
        }
    }
}
