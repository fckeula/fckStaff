package dev.fckeula.fckStaff.commands;

import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.staff.StaffManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FreezeCommand implements BasicCommand {

    private final StaffManager staffManager;
    private final LangManager lang;

    public FreezeCommand(StaffManager staffManager, LangManager lang) {
        this.staffManager = staffManager;
        this.lang = lang;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();

        if (args.length < 1) {
            sender.sendMessage(lang.get("freeze.usage"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(lang.get("freeze.player-not-found"));
            return;
        }

        boolean nowFrozen = !staffManager.isFrozen(target);
        if (!staffManager.setFrozen(target, nowFrozen, sender)) {
            return;
        }

        sender.sendMessage(lang.get(nowFrozen ? "freeze.frozen-staff" : "freeze.unfrozen-staff",
                Placeholder.unparsed("player", target.getName())));
        target.sendMessage(lang.get(nowFrozen ? "freeze.frozen-target" : "freeze.unfrozen-target"));
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("fckstaff.freeze");
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}
