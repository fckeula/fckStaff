package dev.fckeula.fckStaff.listeners;

import dev.fckeula.fckStaff.staff.StaffManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Reinforces vanish beyond simple visibility, since a vanished player's entity
 * still physically exists server-side:
 *  - mobs won't pick a vanished player as a target, or keep hitting one they
 *    were already targeting before the player vanished;
 *  - a vanished player can't trigger pressure plates or tripwires;
 *  - a vanished player can't drop items, so an accidental drop can't give away
 *    their position or get picked up/reacted to by a nearby hopper.
 */
public class VanishListener implements Listener {

    private final StaffManager staffManager;

    public VanishListener(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && staffManager.isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!staffManager.isVanished(player)) return;

        // Only blocks damage from non-player living entities (mobs); PvP between
        // staff and other players isn't this listener's concern.
        if (event.getDamager() instanceof LivingEntity && !(event.getDamager() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPressurePlate(EntityInteractEvent event) {
        if (event.getEntity() instanceof Player player && staffManager.isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (staffManager.isVanished(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
