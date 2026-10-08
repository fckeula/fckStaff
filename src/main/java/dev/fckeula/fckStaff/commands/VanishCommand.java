package dev.fckeula.fckStaff.commands;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements BasicCommand {

    private final FckStaff plugin;
    private final StaffManager staffManager;
    private final LangManager lang;

    public VanishCommand(FckStaff plugin, StaffManager staffManager, LangManager lang) {
        this.plugin = plugin;
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

        boolean requireStaffMode = plugin.getConfig().getBoolean("vanish.require-staffmode", true);
        boolean canBypass = player.hasPermission("fckstaff.vanish.bypass");

        if (requireStaffMode && !staffManager.isInStaffMode(player) && !canBypass) {
            player.sendMessage(lang.get("vanish.requires-staffmode"));
            return;
        }

        Boolean nowVanished = staffManager.toggleVanish(player);
        if (nowVanished == null) return;
        player.sendMessage(lang.get(nowVanished ? "vanish.enabled" : "vanish.disabled"));
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("fckstaff.vanish");
    }
}
