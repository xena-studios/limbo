package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Makes the shared floor layer unbreakable and unmodifiable: nothing can be
 * broken at, placed at, or built below the floor Y. Players build freely above it.
 */
public final class FloorProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public FloorProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        if (event.getBlock().getY() <= plugin.settings().world().floorY()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        // Can't build at or below the floor layer.
        if (event.getBlock().getY() <= plugin.settings().world().floorY()) {
            event.setCancelled(true);
        }
    }
}
