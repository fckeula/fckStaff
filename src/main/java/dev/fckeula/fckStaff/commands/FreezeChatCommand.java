package dev.fckeula.fckStaff.commands;

import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FreezeChatCommand implements BasicCommand {

    private final StaffManager staffManager;
    private final LangManager lang;

    public FreezeChatCommand(StaffManager staffManager, LangManager lang) {
        this.staffManager = staffManager;
        this.lang = lang;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();

        if (args.length < 2) {
            sender.sendMessage(lang.get("freezechat.usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(lang.get("freeze.player-not-found"));
            return;
        }

        if (!staffManager.isFrozen(target)) {
            sender.sendMessage(lang.get("freezechat.not-frozen"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        staffManager.sendFreezeChatFromStaff(sender, target, message);
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("fckstaff.freezechat");
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}
