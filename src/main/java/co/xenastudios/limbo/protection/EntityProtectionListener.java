package co.xenastudios.limbo.protection;

import co.xenastudios.limbo.LimboPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents creative players from spawning entity-heavy items that accumulate:
 * armor stands, boats, minecarts, item frames, paintings, end crystals and
 * spawn-egg mobs.
 */
public final class EntityProtectionListener implements Listener {

    private final LimboPlugin plugin;

    public EntityProtectionListener(LimboPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
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
