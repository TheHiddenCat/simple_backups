package nl.lewin.simpleBackups;

import nl.lewin.simpleBackups.commands.PluginCommands;
import nl.lewin.simpleBackups.commands.PluginCommandsTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SimpleBackups extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        var config = getConfig();
        var pluginConfig = new PluginConfig(config);

        var backupDirectory = new File(getDataFolder(), "backups");
        if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
            getLogger().severe("Failed to create backup directory: " + backupDirectory.getAbsolutePath());
            return;
        }

        var command = getCommand("simplebackups");
        if (command != null) {
            command.setExecutor(new PluginCommands(this, pluginConfig));
            command.setTabCompleter(new PluginCommandsTabCompleter());
        }
        else {
            getLogger().severe("Could not initialize `simplebackups` command because `getCommand` returned null!");
        }
    }
}