package dev.fckeula.fckStaff.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called right before a player's vanish state is toggled (via /vanish, the kit
 * item, or entering/leaving staff mode). Cancel it to prevent the change.
 */
public class VanishToggleEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean vanishing;
    private boolean cancelled;

    public VanishToggleEvent(@NotNull Player player, boolean vanishing) {
        this.player = player;
        this.vanishing = vanishing;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /** True if the player is about to become vanished, false if about to become visible. */
    public boolean isVanishing() {
        return vanishing;
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
