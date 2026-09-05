package com.mahbod.serverstats;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsListener implements Listener {

    private final StatsDatabase db;
    // tracks join time per player so we can compute session playtime on quit
    private final Map<UUID, Long> sessionStart = new HashMap<>();

    public StatsListener(StatsDatabase db) {
        this.db = db;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        long now = System.currentTimeMillis() / 1000L;
        sessionStart.put(p.getUniqueId(), now);
        db.upsertPlayerJoin(p.getUniqueId().toString(), p.getName(), now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        long now = System.currentTimeMillis() / 1000L;
        Long start = sessionStart.remove(p.getUniqueId());
        long session = start != null ? Math.max(0, now - start) : 0;
        db.updateLastSeenAndPlaytime(p.getUniqueId().toString(), now, session);
        db.logEvent("leave", p.getName(), "", now);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        long now = System.currentTimeMillis() / 1000L;
        db.incrementDeath(victim.getUniqueId().toString(), now);

        Player killer = victim.getKiller();
        if (killer != null) {
            db.incrementKill(killer.getUniqueId().toString(), now);
            db.logEvent("kill", killer.getName(), "killed " + victim.getName(), now);
        } else {
            db.logEvent("death", victim.getName(), "died", now);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        db.incrementBlocksBroken(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        db.incrementBlocksPlaced(event.getPlayer().getUniqueId().toString());
    }
}
