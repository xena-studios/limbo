package co.xenastudios.xlimbo.world;

import co.xenastudios.xlimbo.XLimboPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Completes limbo-world setup when the server loads our configured world itself,
 * rather than xLimbo creating it.
 *
 * <p>This is the "default world <em>is</em> the limbo" setup: the server's own
 * world has its generator pointed at xLimbo in {@code bukkit.yml}, so it cannot be
 * created during {@link XLimboPlugin#onEnable()} (the server is still in its
 * STARTUP phase). The server loads it a moment later using
 * {@link XLimboPlugin#getDefaultWorldGenerator}, and we apply gamerules, border and
 * footprint here once it does.
 *
 * <p>Fail-safe: only touches the one matching world, no-ops once setup is done, and
 * never throws out of world load.
 */
public final class LimboWorldLoadListener implements Listener {

    private final XLimboPlugin plugin;

    public LimboWorldLoadListener(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        // handleWorldLoaded is already fully guarded; this catch is a second backstop
        // so a limbo-setup hiccup can never abort the server's world load.
        try {
            plugin.handleWorldLoaded(event.getWorld());
        } catch (Throwable ignored) {
            // Intentionally swallowed — stability over everything.
        }
    }
}
