package dev.fckeula.fckStaff.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called right before a player is frozen or unfrozen. Cancel it to block the
 * action (e.g. to prevent freezing another staff member).
 */
public class PlayerFreezeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player target;
    private final CommandSender initiator;
    private final boolean freezing;
    private boolean cancelled;

    public PlayerFreezeEvent(@NotNull Player target, @Nullable CommandSender initiator, boolean freezing) {
        this.target = target;
        this.initiator = initiator;
        this.freezing = freezing;
    }

    public @NotNull Player getTarget() {
        return target;
    }

    /** Who triggered this freeze/unfreeze. Null if it came from the API/internal logic with no specific sender. */
    public @Nullable CommandSender getInitiator() {
        return initiator;
    }

    /** True if the target is about to be frozen, false if about to be unfrozen. */
    public boolean isFreezing() {
        return freezing;
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
