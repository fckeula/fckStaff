package dev.fckeula.fckStaff.commands;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.lang.LangManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class fckStaffCommand implements BasicCommand {

    private final FckStaff plugin;
    private final LangManager lang;

    public fckStaffCommand(FckStaff plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        CommandSender sender = source.getSender();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            lang.load();
            sender.sendMessage(lang.get("general.reloaded"));
            return;
        }

        sender.sendMessage(lang.get("general.usage"));
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission("fckstaff.admin");
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
