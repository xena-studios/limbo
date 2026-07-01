package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;

/**
 * Prevents any new dimension from ever being generated: cancels nether/end portal
 * creation and blocks using an Eye of Ender on an End Portal Frame.
 */
public final class PortalProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public PortalProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (plugin.isLimboWorld(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEyeOfEnder(PlayerInteractEvent event) {
        // Do not gate on the hand: completing an End portal frame does not fire
        // PortalCreateEvent, so this is the real guard — and an off-hand eye would
        // otherwise slip straight through it.
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.END_PORTAL_FRAME) {
            return;
        }
        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE
                && plugin.isLimboWorld(clicked.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCanBuild(BlockCanBuildEvent event) {
        // Defensive: never allow a nether-portal frame ignition to succeed.
        if (event.getMaterial() == Material.NETHER_PORTAL && plugin.isLimboWorld(event.getBlock().getWorld())) {
            event.setBuildable(false);
        }
    }
}
