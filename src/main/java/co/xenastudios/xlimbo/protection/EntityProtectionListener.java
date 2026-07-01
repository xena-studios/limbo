package co.xenastudios.xlimbo.protection;

import co.xenastudios.xlimbo.XLimboPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents creative players from spawning entity-heavy items that accumulate:
 * armor stands, boats, minecarts, item frames, paintings, end crystals and
 * spawn-egg mobs.
 */
public final class EntityProtectionListener implements Listener {

    private final XLimboPlugin plugin;

    public EntityProtectionListener(XLimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        // Guard both hands: these items can be placed from the off-hand too.
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (!plugin.isLimboWorld(event.getPlayer().getWorld())) {
            return;
        }
        if (isBlockedEntityItem(item.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (plugin.isLimboWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawnEgg(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                && plugin.isLimboWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    private boolean isBlockedEntityItem(Material type) {
        if (type == Material.ARMOR_STAND || type == Material.END_CRYSTAL
                || type == Material.ITEM_FRAME || type == Material.GLOW_ITEM_FRAME
                || type == Material.PAINTING) {
            return true;
        }
        String name = type.name();
        return name.endsWith("_SPAWN_EGG")
                || name.endsWith("_BOAT")
                || name.endsWith("_MINECART")
                || name.equals("MINECART");
    }
}
