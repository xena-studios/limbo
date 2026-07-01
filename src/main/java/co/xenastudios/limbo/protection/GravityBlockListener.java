package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Blocks gravity-affected blocks (sand, gravel, anvils, concrete powder, …) from
 * being placed, and cancels any block→falling-entity conversion, so no
 * {@link FallingBlock} entities are ever created in the sandbox.
 */
public final class GravityBlockListener implements Listener {

    private final LimboPlugin plugin;

    public GravityBlockListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        if (event.getBlock().getType().hasGravity()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFallingSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof FallingBlock && plugin.isLimboWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }
}
