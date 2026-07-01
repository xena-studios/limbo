package co.xenastudios.xlimbo.protection;

import co.xenastudios.xlimbo.XLimboPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional lag-machine defence: caps per-player block placements per second.
 * Uses a cheap fixed-window counter — no allocation on the common path.
 */
public final class BuildGuardListener implements Listener {

    private final XLimboPlugin plugin;
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public BuildGuardListener(XLimboPlugin plugin) {
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Drop the player's window so the map can't grow without bound.
        windows.remove(event.getPlayer().getUniqueId());
    }

    private static final class Window {
        long second = -1L;
        int count = 0;
    }
}
