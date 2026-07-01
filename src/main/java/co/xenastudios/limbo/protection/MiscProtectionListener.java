package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import co.xenastudios.limbo.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Bundles the remaining "quiet world" protections, each gated by its own toggle
 * read live from the settings snapshot: explosions, mob spawns, redstone activity
 * and item drops.
 */
public final class MiscProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public MiscProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.settings().protections().explosions() && plugin.isLimboWorld(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.settings().protections().explosions() && plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.settings().protections().mobSpawns()) {
            return;
        }
        // Allow explicit plugin/custom spawns; block everything natural.
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (plugin.isLimboWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRedstone(BlockRedstoneEvent event) {
        if (plugin.settings().protections().redstone() && plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setNewCurrent(0);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Settings.Protections p = plugin.settings().protections();
        if (p.itemDrops() && plugin.isLimboWorld(event.getPlayer().getWorld())) {
            event.setCancelled(true);
        }
    }
}
