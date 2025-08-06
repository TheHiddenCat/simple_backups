package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.lewin.simpleBackups.PluginConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class PluginCommands implements CommandExecutor {
    private final @NotNull Map<String, SubCommand> subCommands;

    public PluginCommands(@NotNull final Plugin plugin, @NotNull final PluginConfig config) {
        subCommands = new HashMap<>();
        registerSubcommand(new BackupCommand(plugin, config));
        registerSubcommand(new HelpCommand());
    }

    private void registerSubcommand(@NotNull final SubCommand sub) {
        subCommands.put(sub.name(), sub);
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                             @NotNull final String label, final String @NotNull [] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, args);
            return true;
        }

        var subName = args[0].toLowerCase();
        var sub = subCommands.get(subName);

        if (sub != null) {
            sub.execute(sender, args);
        } else {
            sender.sendMessage(Component.text("Unknown subcommand. Use /simplebackups help",
                    NamedTextColor.RED));
        }

        return true;
    }
}