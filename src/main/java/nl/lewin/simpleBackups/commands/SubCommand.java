package nl.lewin.simpleBackups.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface SubCommand {
    void execute(@NotNull CommandSender sender, @NotNull String[] args);
    @NotNull String name();
}