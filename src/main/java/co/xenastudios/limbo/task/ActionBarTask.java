package co.xenastudios.limbo.task;

import co.xenastudios.limbo.LimboPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Repeating action-bar broadcast. The message {@link Component} is pre-parsed in
 * the settings snapshot, so each tick only iterates players and sends — no
 * MiniMessage parsing on the loop.
 */
public final class ActionBarTask {

    private final LimboPlugin plugin;

    public ActionBarTask(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    /** Schedule the repeating task and return it for lifecycle tracking. */
    public BukkitTask start() {
        int interval = Math.max(1, plugin.settings().actionBar().intervalTicks());
        return plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    private void tick() {
        try {
            Component message = plugin.settings().actionBar().message();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendActionBar(message);
            }
        } catch (Throwable ignored) {
            // Never let a throwing tick cancel the repeating task.
        }
    }
}
