package co.xenastudios.limbo.player;

import co.xenastudios.limbo.LimboPlugin;
import co.xenastudios.limbo.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Core join flow: put the player into creative + flight, teleport them to a
 * bounded random spawn without blocking the join tick, apply the optional custom
 * join message / chat clear, and schedule the optional auto-connect.
 */
public final class JoinListener implements Listener {

    private final LimboPlugin plugin;

    public JoinListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Settings settings = plugin.settings();
        Player player = event.getPlayer();

        try {
            if (settings.join().suppressJoinMessage()) {
                event.joinMessage(null);
            }

            applyPlayerState(player, settings);
            teleportToSandbox(player, settings);

            Component custom = settings.join().customJoinMessage();
            if (custom != null) {
                player.sendMessage(custom);
            }
            if (settings.join().clearChatOnJoin()) {
                clearChat(player);
            }

            plugin.autoJoinScheduler().schedule(player);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Join handling failed for " + player.getName(), t);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        // Clean up any pending auto-connect so it never fires for an offline player,
        // and drop the cooldown entry so the map can't grow without bound.
        plugin.autoJoinScheduler().cancel(id);
        plugin.cooldownManager().clear(id);
    }

    /** Force creative + flight so a player can never end up in the void without flight. */
    private void applyPlayerState(Player player, Settings settings) {
        if (settings.player().setCreative()) {
            player.setGameMode(GameMode.CREATIVE);
        }
        if (settings.player().enableFlight()) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    /** Async spawn: preload the destination chunk, then async-teleport onto the floor. */
    private void teleportToSandbox(Player player, Settings settings) {
        World world = plugin.limboWorld();
        if (world == null) {
            return; // world unavailable — leave the player where they are rather than erroring
        }
        SpawnLocator.locateAsync(world, settings).thenAccept(location -> {
            try {
                player.teleportAsync(location).thenAccept(ok -> {
                    // Re-assert flight after the teleport lands.
                    if (settings.player().enableFlight() && player.isOnline()) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }
                });
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Spawn teleport failed for " + player.getName(), t);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "Spawn chunk preload failed for " + player.getName(), ex);
            return null;
        });
    }

    private void clearChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage(Component.empty());
        }
    }
}
