package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.lewin.simpleBackups.compression.ZipCompression;
import nl.lewin.simpleBackups.tasks.BackupTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class BackupCommand implements SubCommand {
    private final @NotNull Plugin plugin;
    private final @NotNull Logger logger;
    private static final @NotNull AtomicBoolean running = new AtomicBoolean(false);

    public BackupCommand(@NotNull final Plugin plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
    }

    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage(Component.text("Backup is already running", NamedTextColor.RED));
            return;
        }

        var worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            sender.sendMessage(Component.text("No loaded worlds to back up.", NamedTextColor.RED));
            return;
        }

        plugin.getServer().broadcast(Component.text("[!] Server backup has started", NamedTextColor.YELLOW));

        for (var world : worlds) {
            world.setAutoSave(false);
            world.save();
            logger.info("Flushed and disabled autosave for world: " + world.getName());
        }

        new BackupTask(
                logger,
                getBackupDirectory(),
                new ZipCompression(logger),
                () -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (var world : Bukkit.getWorlds()) {
                            world.setAutoSave(true);
                            logger.info("Re-enabled autosave for world: " + world.getName());
                        }
                    });
                    running.set(false);
                    plugin.getServer().broadcast(Component.text("Server backup completed", NamedTextColor.GREEN));
                }
        ).runTaskAsynchronously(plugin);
    }

    @Override
    public @NotNull String name() {
        return "backup";
    }

    private @NotNull File getBackupDirectory() {
        return new File(plugin.getDataFolder(), "backups");
    }
}
