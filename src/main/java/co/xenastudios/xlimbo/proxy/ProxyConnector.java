package co.xenastudios.xlimbo.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.logging.Level;

/**
 * Sends players to another server on the proxy using the standard BungeeCord
 * {@code Connect} plugin message (understood by both BungeeCord and Velocity).
 *
 * <p><b>Security note (server-side):</b> the proxy must be configured with secure
 * forwarding (Velocity modern forwarding or a BungeeCord guard) so clients cannot
 * spoof this message. The plugin cannot enforce that — see the README.
 */
public final class ProxyConnector {

    public static final String CHANNEL = "BungeeCord";

    private final Plugin plugin;

    public ProxyConnector(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Register the outgoing plugin channel. Safe to call once on enable. */
    public void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    public void unregister() {
        try {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        } catch (Throwable ignored) {
            // Nothing to clean up if it was never registered.
        }
    }

    /**
     * Ask the proxy to move {@code player} to {@code serverName}. Fail-safe:
     * returns false and logs on any error rather than propagating.
     */
    public boolean send(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to send Connect plugin message for " + player.getName(), t);
            return false;
        }
    }
}
