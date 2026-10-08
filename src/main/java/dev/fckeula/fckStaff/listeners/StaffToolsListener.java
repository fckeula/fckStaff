package dev.fckeula.fckStaff.listeners;

import dev.fckeula.fckStaff.gui.StaffToolsMenu;
import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class StaffToolsListener implements Listener {

    private final StaffManager staffManager;
    private final StaffToolsMenu toolsMenu;
    private final LangManager lang;
    private final Random random = new Random();

    public StaffToolsListener(StaffManager staffManager, StaffToolsMenu toolsMenu, LangManager lang) {
        this.staffManager = staffManager;
        this.toolsMenu = toolsMenu;
        this.lang = lang;
    }

    private String getTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(StaffToolsMenu.TOOL_KEY, PersistentDataType.STRING);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Paper fires this event once per hand (main + off-hand) for a single click.
        // Without this check, toggle-style tools (like the freeze stick) would run twice.
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!staffManager.isInStaffMode(player)) return;

        String tool = getTool(event.getItem());
        if (tool == null) return;

        event.setCancelled(true);

        switch (tool) {
            case StaffToolsMenu.RANDOM_TP -> randomTeleport(player);
            case StaffToolsMenu.MENU_OPENER -> player.openInventory(toolsMenu.buildMenu());
            case StaffToolsMenu.VANISH_TOGGLE -> {
                Boolean nowVanished = staffManager.toggleVanish(player);
                if (nowVanished != null) {
                    player.sendMessage(lang.get(nowVanished ? "vanish.enabled" : "vanish.disabled"));
                }
            }
            case StaffToolsMenu.FLY_TOGGLE -> {
                boolean nowFlying = !player.getAllowFlight();
                player.setAllowFlight(nowFlying);
                if (!nowFlying) {
                    player.setFlying(false);
                }
                player.sendMessage(lang.get(nowFlying ? "fly.enabled" : "fly.disabled"));
            }
            default -> {}
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // Same double-fire issue as onInteract - only handle the main hand.
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!staffManager.isInStaffMode(player)) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        String tool = getTool(player.getInventory().getItemInMainHand());
        if (tool == null) return;

        event.setCancelled(true);

        switch (tool) {
            case StaffToolsMenu.FREEZE_STICK -> toggleFreeze(player, target);
            case StaffToolsMenu.INSPECTOR -> player.openInventory(target.getInventory());
            default -> {}
        }
    }

    private void toggleFreeze(Player staff, Player target) {
        if (!staff.hasPermission("fckstaff.freeze")) {
            staff.sendMessage(lang.get("freeze.no-permission"));
            return;
        }
        boolean nowFrozen = !staffManager.isFrozen(target);
        if (!staffManager.setFrozen(target, nowFrozen, staff)) {
            return;
        }
        staff.sendMessage(lang.get(nowFrozen ? "freeze.frozen-staff" : "freeze.unfrozen-staff",
                Placeholder.unparsed("player", target.getName())));
        target.sendMessage(lang.get(nowFrozen ? "freeze.frozen-target" : "freeze.unfrozen-target"));
    }

    private void randomTeleport(Player player) {
        List<? extends Player> candidates = player.getServer().getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> !staffManager.isInStaffMode(p))
                .toList();

        if (candidates.isEmpty()) {
            player.sendMessage(lang.get("tools.random-tp-none"));
            return;
        }

        Player target = candidates.get(random.nextInt(candidates.size()));
        player.teleport(target.getLocation());
        player.sendMessage(lang.get("tools.random-tp-success", Placeholder.unparsed("player", target.getName())));
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(toolsMenu.menuTitle())) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = clicked.getItemMeta().getPersistentDataContainer()
                .get(StaffToolsMenu.MENU_ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case StaffToolsMenu.ACTION_STAFF_LIST -> {
                player.closeInventory();
                StringBuilder sb = new StringBuilder();
                player.getServer().getOnlinePlayers().stream()
                        .filter(staffManager::isInStaffMode)
                        .forEach(p -> sb.append(p.getName()).append(" "));
                String list = sb.isEmpty() ? "-" : sb.toString().trim();
                player.sendMessage(lang.get("tools.staff-online", Placeholder.unparsed("list", list)));
            }
            case StaffToolsMenu.ACTION_RANDOM_TP -> {
                player.closeInventory();
                randomTeleport(player);
            }
            case StaffToolsMenu.ACTION_LEAVE -> {
                player.closeInventory();
                staffManager.disableStaffMode(player);
            }
            default -> {}
        }
    }
}
