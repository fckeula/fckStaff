package dev.fckeula.fckStaff.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called right before a message is broadcast to the staff chat channel. Other
 * plugins can rewrite the message (e.g. to filter it or relay it elsewhere) or
 * cancel it entirely.
 */
public class StaffChatMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private String message;
    private boolean cancelled;

    public StaffChatMessageEvent(@NotNull Player sender, @NotNull String message) {
        this.sender = sender;
        this.message = message;
    }

    public @NotNull Player getSender() {
        return sender;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public void setMessage(@NotNull String message) {
        this.message = message;
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
