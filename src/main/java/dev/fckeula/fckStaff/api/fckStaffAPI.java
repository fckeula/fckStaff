package dev.fckeula.fckStaff.api;

import dev.fckeula.fckStaff.api.event.PlayerFreezeEvent;
import dev.fckeula.fckStaff.api.event.StaffModeToggleEvent;
import dev.fckeula.fckStaff.api.event.VanishToggleEvent;
import org.bukkit.entity.Player;

/**
 * Public API for other plugins to read and control fckStaff's state.
 *
 * <p>Obtain an instance via Bukkit's ServicesManager:
 * <pre>{@code
 * RegisteredServiceProvider<fckStaffAPI> provider =
 *         Bukkit.getServicesManager().getRegistration(fckStaffAPI.class);
 * if (provider != null) {
 *     fckStaffAPI api = provider.getProvider();
 * }
 * }</pre>
 *
 * <p>This interface depends only on Bukkit types, so it's safe to compile
 * against without pulling in any of fckStaff's internals.
 */
public interface fckStaffAPI {

    boolean isInStaffMode(Player player);

    boolean isVanished(Player player);

    boolean isFrozen(Player player);

    boolean isStaffChatToggled(Player player);

    /**
     * Enables or disables staff mode for a player.
     *
     * @return true if the change was applied; false if the player was already
     *         in that state, or another plugin cancelled it via {@link StaffModeToggleEvent}.
     */
    boolean setStaffMode(Player player, boolean enabled);

    /**
     * Freezes or unfreezes a player.
     *
     * @return true if the change was applied; false if the player was already
     *         in that state, or another plugin cancelled it via {@link PlayerFreezeEvent}.
     */
    boolean setFrozen(Player player, boolean frozen);

    /**
     * Enables or disables vanish for a player, independent of staff mode.
     *
     * @return true if the change was applied; false if the player was already
     *         in that state, or another plugin cancelled it via {@link VanishToggleEvent}.
     */
    boolean setVanish(Player player, boolean vanished);
}
