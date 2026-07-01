package co.xenastudios.limbo.player;

import co.xenastudios.limbo.LimboPlugin;
import co.xenastudios.limbo.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Schedules the optional delayed auto-connect back to the main server and cleanly
 * cancels it if the player disconnects first. Owned by the plugin (not recreated
 * on reload) so no per-player task is ever leaked.
 */
public final class AutoJoinScheduler {

    private final LimboPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new ConcurrentHashMap<>();

    public AutoJoinScheduler(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    /** Schedule an auto-connect for {@code player} using the current settings. */
    public void schedule(Player player) {
        Settings settings = plugin.settings();
        Settings.AutoJoin cfg = settings.autoJoin();
        if (!cfg.enabled()) {
            return;
        }
        cancel(player.getUniqueId()); // never double-schedule

        long delayTicks = Math.max(1L, cfg.delaySeconds() * 20L);
        UUID id = player.getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            try {
                Player online = plugin.getServer().getPlayer(id);
                if (online == null || !online.isOnline()) {
                    return;
                }
                Component msg = cfg.message();
                if (msg != null) {
                    online.sendMessage(msg);
                }
                plugin.proxyConnector().send(online, settings.mainServer());
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Auto-join task failed.", t);
            }
        }, delayTicks);
        pending.put(id, task);
    }

    /** Cancel any pending auto-connect for a player (e.g. on quit). */
    public void cancel(UUID player) {
        BukkitTask task = pending.remove(player);
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // Already run or cancelled.
            }
        }
    }

    public void cancelAll() {
        for (BukkitTask task : pending.values()) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // ignore
            }
        }
        pending.clear();
    }
}
