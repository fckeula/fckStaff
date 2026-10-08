package dev.fckeula.fckStaff.listeners;

import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener implements Listener {

    private final StaffManager staffManager;
    private final LangManager lang;

    public FreezeListener(StaffManager staffManager, LangManager lang) {
        this.staffManager = staffManager;
        this.lang = lang;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!staffManager.isFrozen(player)) return;

        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
            player.sendMessage(lang.get("freeze.cant-move"));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!staffManager.isFrozen(player)) return;

        String command = event.getMessage().toLowerCase();
        if (!command.startsWith("/msg") && !command.startsWith("/tell") && !command.startsWith("/r ")) {
            event.setCancelled(true);
            player.sendMessage(lang.get("freeze.cant-command"));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (staffManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (staffManager.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Invincibility: cancels any damage taken while frozen, regardless of source.
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && staffManager.isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    // Prevents a frozen player from dealing damage to others (melee or ranged).
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker != null && staffManager.isFrozen(attacker)) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    // Blocks opening chests, furnaces, shulkers, etc. while frozen. Their own
    // inventory/crafting screens stay allowed since those don't affect the world.
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!staffManager.isFrozen(player)) return;

        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING) return;

        event.setCancelled(true);
        player.sendMessage(lang.get("freeze.cant-interact"));
    }
}
