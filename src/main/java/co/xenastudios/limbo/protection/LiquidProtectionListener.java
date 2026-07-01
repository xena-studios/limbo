package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Keeps the sandbox liquid-free: cancels bucket placement and any liquid flow
 * ({@link BlockFromToEvent}) inside the limbo world.
 */
public final class LiquidProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public LiquidProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }
}
