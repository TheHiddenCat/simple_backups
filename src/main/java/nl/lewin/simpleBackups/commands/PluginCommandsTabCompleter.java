package nl.lewin.simpleBackups.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class PluginCommandsTabCompleter implements TabCompleter {

    private static @NotNull final List<String> SUBCOMMANDS = List.of(
            "backup", "help"
    );

    @Override
    public @NotNull List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                               @NotNull final String s, @NotNull final String @NotNull [] args) {
        if (args.length == 1) {
            var list = new ArrayList<String>();
            for (var sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
            return list;
        }

        return new ArrayList<>();
    }
}
