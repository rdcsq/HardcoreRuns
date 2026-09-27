package com.rdcsq.hardcoreruns;

import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;

import java.util.concurrent.Semaphore;

public class HardcoreRunsPlugin extends JavaPlugin implements Listener {
    private MultiverseCoreApi multiverseCoreApi = null;
    private final Semaphore semaphore = new Semaphore(1);

    @Override
    public void onEnable() {
        super.onEnable();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        this.multiverseCoreApi = MultiverseCoreApi.get();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!semaphore.tryAcquire()) return;

        var worldName = "world_" + System.currentTimeMillis() / 1000L;

        for (var player : this.getServer().getOnlinePlayers()) {
            player.sendMessage("Starting a new run...");
        }

        multiverseCoreApi.getWorldManager()
                .createWorld(CreateWorldOptions.worldName(worldName))
                .onFailure(failure -> {
                    for (var player : this.getServer().getOnlinePlayers()) {
                        player.sendMessage("Failed to create new world. " + failure.toString());
                    }
                    semaphore.release();
                })
                .onSuccess(world -> {
                    this.getConfig().set("current_run", worldName);
                    this.saveConfig();

                    for (var player : this.getServer().getOnlinePlayers()) {
                        if (player == event.getPlayer()) continue;

                        // intentional game design!
                        var damageSource = DamageSource.builder(DamageType.BAD_RESPAWN_POINT)
                                .withDamageLocation(player.getLocation())
                                .build();

                        player.damage(Double.MAX_VALUE, damageSource);
                    }
                    semaphore.release();
                });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(getCurrentWorld().getSpawnLocation());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var currentWorld = getCurrentWorld();

        if (event.getPlayer().getWorld() != currentWorld) {
            event.getPlayer().sendMessage("A new run has started since the last time you joined.");
            event.getPlayer().setRespawnLocation(currentWorld.getSpawnLocation());
            event.getPlayer().teleport(currentWorld.getSpawnLocation());
        }
    }

    private World getCurrentWorld() {
        var currentWorld = this.getConfig().getString("current_run");
        if (currentWorld == null) {
            currentWorld = "world";
        }
        return this.getServer().getWorld(currentWorld);
    }
}
