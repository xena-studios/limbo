package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

/**
 * Disables fire: cancels ignition, spread and burning. (The world's
 * {@code doFireTick} gamerule is also set false at world load.)
 */
public final class FireProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public FireProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }
}
