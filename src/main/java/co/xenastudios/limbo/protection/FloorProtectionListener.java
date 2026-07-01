package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * Makes the shared floor layer unbreakable and unmodifiable: nothing can be
 * broken at, placed at, or built below the floor Y, and no piston can push or
 * pull a block into or out of the floor layer. Players build freely above it.
 */
public final class FloorProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public FloorProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        if (event.getBlock().getY() <= plugin.settings().world().floorY()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.isLimboWorld(event.getBlock().getWorld())) {
            return;
        }
        // Can't build at or below the floor layer.
        if (event.getBlock().getY() <= plugin.settings().world().floorY()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (touchesFloor(event.getBlock().getWorld(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (touchesFloor(event.getBlock().getWorld(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    /** True if any moved block sits at/below the floor, or would move there. */
    private boolean touchesFloor(World world, List<Block> moved, BlockFace direction) {
        if (!plugin.isLimboWorld(world)) {
            return false;
        }
        int floorY = plugin.settings().world().floorY();
        for (Block block : moved) {
            if (block.getY() <= floorY || block.getRelative(direction).getY() <= floorY) {
                return true;
            }
        }
        return false;
    }
}
