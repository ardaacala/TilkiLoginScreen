package com.tilkicraft.login;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TilkiLoginScreen extends JavaPlugin implements Listener {
    private static final UUID PACK_ID = UUID.fromString("0c0dadbb-d83d-43d0-9505-6a0f668c6acc");
    // Total advance 36; black quad and logo share their horizontal center.
    private static final String OVERLAY = "\uE902\uE900\uE904\uE903\uE901\uE904";
    private final Map<UUID, Session> sessions = new HashMap<>();
    private Object authApi;
    private Method authenticated;
    private Method unrestricted;
    private String packUrl;
    private byte[] hash;
    private long tick;

    private static final class Session {
        final long requestAt;
        boolean requested;
        boolean loaded;
        boolean visible;
        Session(long requestAt) { this.requestAt = requestAt; }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        String oldUrl = getConfig().getString("pack-url", "");
        if (oldUrl.equals("https://github.com/ardaacala/TilkiLoginScreen/releases/download/v1.0.0/TilkiLoginScreen-ResourcePack.zip")) {
            getConfig().set("pack-url", "https://github.com/ardaacala/TilkiLoginScreen/releases/download/v1.0.1/TilkiLoginScreen-ResourcePack.zip");
            getConfig().set("pack-sha1", "148670db4f58e1a097edc70acac98715fdfc721d");
            saveConfig();
        }
        try {
            var auth = Bukkit.getPluginManager().getPlugin("AuthMe");
            if (auth == null || !auth.isEnabled()) throw new IllegalStateException("AuthMe is not enabled");
            Class<?> apiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi", true, auth.getClass().getClassLoader());
            authApi = apiClass.getMethod("getInstance").invoke(null);
            if (authApi == null) throw new IllegalStateException("AuthMe API is not ready");
            authenticated = apiClass.getMethod("isAuthenticated", Player.class);
            unrestricted = apiClass.getMethod("isUnrestricted", Player.class);
            packUrl = getConfig().getString("pack-url", "").trim();
            if (!SharedPack.active() && !packUrl.startsWith("https://")) throw new IllegalArgumentException("Set pack-url to the published HTTPS ZIP URL");
            hash = HexFormat.of().parseHex(getConfig().getString("pack-sha1", ""));
            if (!SharedPack.active() && hash.length != 20) throw new IllegalArgumentException("pack-sha1 must contain 40 hexadecimal characters");
        } catch (Exception ex) {
            getLogger().severe("Logo screen disabled: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::update, 1, 5);
        for (Player player : Bukkit.getOnlinePlayers()) track(player);
    }

    private void track(Player player) {
        sessions.put(player.getUniqueId(), new Session(tick + Math.max(1, getConfig().getLong("pack-delay-ticks", 60))));
    }

    @EventHandler public void join(PlayerJoinEvent event) { track(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) { sessions.remove(event.getPlayer().getUniqueId()); }

    @EventHandler public void packStatus(PlayerResourcePackStatusEvent event) {
        if (!SharedPack.matches(PACK_ID,event.getID())) return;
        Session state = sessions.get(event.getPlayer().getUniqueId());
        if (state == null) return;
        state.loaded = event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        if (!state.loaded && state.visible) clear(event.getPlayer(), state);
    }

    private void update() {
        tick += 5;
        try {
            for (var entry : sessions.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;
                Session state = entry.getValue();
                if (SharedPack.active()) state.loaded=SharedPack.loaded(player);
                boolean loggedIn = (boolean) authenticated.invoke(authApi, player)
                        || (boolean) unrestricted.invoke(authApi, player);
                if (loggedIn) {
                    clear(player, state);
                    continue;
                }
                if (!SharedPack.active() && !state.requested && tick >= state.requestAt) {
                    player.addResourcePack(PACK_ID, packUrl, hash,
                            getConfig().getString("pack-prompt"), getConfig().getBoolean("pack-required", true));
                    state.requested = true;
                }
                if (state.loaded) {
                    player.sendTitle(OVERLAY, "", 0, 15, 0);
                    state.visible = true;
                }
            }
        } catch (Exception ex) {
            getLogger().severe("AuthMe API failed; removing overlay: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void clear(Player player, Session state) {
        if (state.visible) player.resetTitle();
        state.visible = false;
    }

    @Override public void onDisable() {
        for (var entry : sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) clear(player, entry.getValue());
        }
        sessions.clear();
    }
}
