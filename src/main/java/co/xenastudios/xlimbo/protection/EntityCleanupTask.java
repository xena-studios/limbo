package co.xenastudios.xlimbo.protection;

import co.xenastudios.xlimbo.XLimboPlugin;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodically removes stray, non-player entities from the limbo world to prevent
 * unbounded accumulation (dropped items, arrows, orphaned mobs, falling blocks).
 * With {@code block-entity-items} enabled (default) players can't place persistent
 * entities, so this mostly clears transient junk.
 */
public final class EntityCleanupTask {

    private final XLimboPlugin plugin;

    public EntityCleanupTask(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask start() {
        long period = Math.max(1L, plugin.settings().protections().entities().cleanupIntervalSeconds() * 20L);
        return plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    private void tick() {
        try {
            World world = plugin.limboWorld();
            if (world == null) {
                return;
            }
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
            }
        } catch (Throwable ignored) {
            // Never let a throwing tick cancel the repeating task.
        }
    }
}
