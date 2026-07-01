package co.xenastudios.xlimbo.player;

import co.xenastudios.xlimbo.XLimboPlugin;
import co.xenastudios.xlimbo.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Suppresses vanilla quit and death broadcasts (join suppression lives in
 * {@link JoinListener}). All toggles read from the immutable settings snapshot.
 */
public final class MessageListener implements Listener {

    private final XLimboPlugin plugin;

    public MessageListener(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Settings settings = plugin.settings();
        if (settings.messages().suppressQuitMessage()) {
            event.quitMessage(null);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Settings settings = plugin.settings();
        if (settings.messages().suppressDeathMessage()) {
            event.deathMessage(null);
        }
    }
}
