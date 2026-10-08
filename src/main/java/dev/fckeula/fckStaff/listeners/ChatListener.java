package dev.fckeula.fckStaff.listeners;

import dev.fckeula.fckStaff.staff.StaffManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final StaffManager staffManager;

    public ChatListener(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        boolean frozen = staffManager.isFrozen(player);
        if (!frozen && !staffManager.hasStaffChatToggled(player)) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (frozen) {
            staffManager.sendFreezeChatFromFrozen(player, message);
        } else {
            staffManager.broadcastStaffChat(player, message);
        }
    }
}
