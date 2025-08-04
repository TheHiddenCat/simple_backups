package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class HelpCommand implements SubCommand {
    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Component.text("[SimpleBackups]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/simplebackups backup", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/simplebackups help", NamedTextColor.GRAY));
    }

    @Override
    public @NotNull String name() {
        return "help";
    }
}
