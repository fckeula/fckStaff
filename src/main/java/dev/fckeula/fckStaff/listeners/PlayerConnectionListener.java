package dev.fckeula.fckStaff.listeners;

import dev.fckeula.fckStaff.staff.StaffManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final StaffManager staffManager;

    public PlayerConnectionListener(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        staffManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Staff mode is intentionally kept active on quit - see StaffManager#handleQuit
        staffManager.handleQuit(event.getPlayer());
    }
}
