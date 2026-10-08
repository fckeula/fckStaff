package dev.fckeula.fckStaff.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called right before a player's staff mode is toggled. Cancel it to prevent
 * the change (e.g. to block entering staff mode while in combat).
 */
public class StaffModeToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean enabling;
    private boolean cancelled;

    public StaffModeToggleEvent(@NotNull Player player, boolean enabling) {
        this.player = player;
        this.enabling = enabling;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /** True if staff mode is about to be enabled, false if it's about to be disabled. */
    public boolean isEnabling() {
        return enabling;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
