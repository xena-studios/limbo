package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional lag-machine defence: caps per-player block placements per second.
 * Uses a cheap fixed-window counter — no allocation on the common path.
 */
public final class BuildGuardListener implements Listener {

    private final LimboPlugin plugin;
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public BuildGuardListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        int max = plugin.settings().protections().buildGuard().maxPlacesPerSecond();
        if (max <= 0) {
            return;
        }
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        long nowSecond = System.currentTimeMillis() / 1000L;
        Window window = windows.computeIfAbsent(id, k -> new Window());
        synchronized (window) {
            if (window.second != nowSecond) {
                window.second = nowSecond;
                window.count = 0;
            }
            window.count++;
            if (window.count > max) {
                event.setCancelled(true);
            }
        }
    }

    private static final class Window {
        long second = -1L;
        int count = 0;
    }
}
