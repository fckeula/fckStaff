package dev.fckeula.fckStaff.api;

import dev.fckeula.fckStaff.staff.StaffManager;
import org.bukkit.entity.Player;

/**
 * Thin wrapper around {@link StaffManager} exposed to other plugins via the
 * ServicesManager. Kept separate from fckStaffAPI itself so the public
 * contract never leaks internal types.
 */
public class fckStaffAPIImpl implements fckStaffAPI {

    private final StaffManager staffManager;

    public fckStaffAPIImpl(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @Override
    public boolean isInStaffMode(Player player) {
        return staffManager.isInStaffMode(player);
    }

    @Override
    public boolean isVanished(Player player) {
        return staffManager.isVanished(player);
    }

    @Override
    public boolean isFrozen(Player player) {
        return staffManager.isFrozen(player);
    }

    @Override
    public boolean isStaffChatToggled(Player player) {
        return staffManager.hasStaffChatToggled(player);
    }

    @Override
    public boolean setStaffMode(Player player, boolean enabled) {
        return enabled ? staffManager.enableStaffMode(player) : staffManager.disableStaffMode(player);
    }

    @Override
    public boolean setFrozen(Player player, boolean frozen) {
        return staffManager.setFrozen(player, frozen, null);
    }

    @Override
    public boolean setVanish(Player player, boolean vanished) {
        if (staffManager.isVanished(player) == vanished) return false;
        Boolean result = staffManager.toggleVanish(player);
        return result != null;
    }
}
