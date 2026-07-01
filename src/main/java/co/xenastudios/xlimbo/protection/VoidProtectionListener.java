package co.xenastudios.xlimbo.protection;

import co.xenastudios.xlimbo.XLimboPlugin;
import co.xenastudios.xlimbo.config.Settings;
import co.xenastudios.xlimbo.player.SpawnLocator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rescues players who drop below the configured Y threshold, teleporting them back
 * onto the floor without a death loop. Optionally cancels void/fall damage.
 *
 * <p>The move handler is written to bail out in one cheap comparison on the common
 * path (player above the threshold), and a per-player guard prevents teleport spam
 * while an async rescue is in flight.
 */
public final class VoidProtectionListener implements Listener {

    private final XLimboPlugin plugin;
    private final Set<UUID> rescuing = ConcurrentHashMap.newKeySet();

    public VoidProtectionListener(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Settings.Protections.Void cfg = plugin.settings().protections().voidRescue();
        // The listener may be registered purely to cancel void damage; only the
        // move-based rescue is gated by the rescue toggle.
        if (!cfg.enabled()) {
            return;
        }
        // Fast path: most moves are well above the threshold.
        if (event.getTo().getY() >= cfg.thresholdY()) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.isLimboWorld(player.getWorld())) {
            return;
        }
        rescue(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Settings.Protections.Void cfg = plugin.settings().protections().voidRescue();
        if (!cfg.cancelVoidDamage()) {
            return;
        }
        if (!plugin.isLimboWorld(player.getWorld())) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            if (cause == EntityDamageEvent.DamageCause.VOID) {
                rescue(player);
            }
        }
    }

    private void rescue(Player player) {
        UUID id = player.getUniqueId();
        if (!rescuing.add(id)) {
            return; // a rescue is already in progress for this player
        }
        try {
            World world = plugin.limboWorld();
            if (world == null) {
                rescuing.remove(id);
                return;
            }
            int floorY = plugin.settings().world().floorY();
            Location target = SpawnLocator.toLanding(world,
                    player.getLocation().getBlockX(), player.getLocation().getBlockZ(), floorY);
            player.teleportAsync(target).whenComplete((ok, err) -> rescuing.remove(id));
        } catch (Throwable t) {
            rescuing.remove(id);
            plugin.getLogger().warning("Void rescue failed for " + player.getName() + ": " + t.getMessage());
        }
    }
}
