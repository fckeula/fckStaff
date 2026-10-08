package dev.fckeula.fckStaff.commands;

import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StaffModeCommand implements BasicCommand {

    private final StaffManager staffManager;
    private final LangManager lang;

    public StaffModeCommand(StaffManager staffManager, LangManager lang) {
        this.staffManager = staffManager;
        this.lang = lang;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("staffmode.player-only"));
            return;
        }
        staffManager.toggleStaffMode(player);
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("fckstaff.staffmode");
    }
}
