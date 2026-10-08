package dev.fckeula.fckStaff.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import dev.fckeula.fckStaff.staff.StaffManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Paper's Player#hidePlayer already removes vanished players from the tab list and
 * entity list. This listener plugs the remaining leak: command/chat name
 * autocompletion (e.g. "/msg <tab>") still suggests them for anyone without
 * fckstaff.see.
 */
public class TabCompleteListener implements Listener {

    private final StaffManager staffManager;

    public TabCompleteListener(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.hasPermission("fckstaff.see")) return;

        Set<String> vanishedNames = player.getServer().getOnlinePlayers().stream()
                .filter(staffManager::isVanished)
                .map(p -> p.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (vanishedNames.isEmpty()) return;

        List<String> filtered = event.getCompletions().stream()
                .filter(completion -> !vanishedNames.contains(completion.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        event.setCompletions(filtered);
    }
}
