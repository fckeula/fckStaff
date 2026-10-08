package dev.fckeula.fckStaff.placeholders;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.staff.StaffManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %fckstaff_<placeholder>% for PlaceholderAPI. Only ever loaded and
 * instantiated by FckStaff when PlaceholderAPI is actually installed - see
 * FckStaff#onEnable for the guard that keeps this optional.
 */
public class fckStaffPlaceholders extends PlaceholderExpansion {

    private final FckStaff plugin;
    private final StaffManager staffManager;

    public fckStaffPlaceholders(FckStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "fckstaff";
    }

    @Override
    public @NotNull String getAuthor() {
        return "iiaan";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // Keeps the expansion registered across /papi reload - it only reads live
    // state from StaffManager, it doesn't hold anything that could go stale.
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        return switch (params.toLowerCase()) {
            case "staffmode" -> bool(staffManager.isInStaffMode(player));
            case "vanish" -> bool(staffManager.isVanished(player));
            case "frozen" -> bool(staffManager.isFrozen(player));
            case "staffchat" -> bool(staffManager.hasStaffChatToggled(player));
            default -> null;
        };
    }

    private String bool(boolean value) {
        return value ? "true" : "false";
    }
}
