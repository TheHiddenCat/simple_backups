package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.lewin.simpleBackups.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupCommand implements SubCommand {
    private static final @NotNull AtomicBoolean running = new AtomicBoolean(false);
    private final @NotNull Plugin plugin;
    private final @NotNull Logger logger;
    private final @NotNull PluginConfig config;
    private static final int BUFFER_SIZE = 65536; // 64KB

    public BackupCommand(@NotNull final Plugin plugin, @NotNull final PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        logger = plugin.getLogger();
    }

    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        if (!running.compareAndSet(false, true)) {
            sender.sendMessage(Component.text("Backup is already running", NamedTextColor.RED));
            return;
        }

        plugin.getServer().broadcast(Component.text("[!] Server backup has started", NamedTextColor.YELLOW));

        var worlds = Bukkit.getWorlds();
        for (var world : worlds) {
            world.save();
            world.setAutoSave(false);
            logger.info("Saved world: " + world.getName());
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            player.saveData();
            logger.info("Saved player with UUID: " + player.getUniqueId());
        }

        var backupFolder = plugin.getDataFolder().toPath().resolve("backups");
        var fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()) + ".zip";
        var backupZip = backupFolder.resolve(fileName);

        try {
            Files.createDirectories(backupFolder);
            Files.createFile(backupZip);
        } catch (IOException e) {
            logger.severe("Failed to create essential directories for backup: " + e.getMessage());
            return;
        }

        cleanupBackups(backupFolder);

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try (var bos = new BufferedOutputStream(Files.newOutputStream(backupZip, StandardOpenOption.WRITE), BUFFER_SIZE);
                 var zos = new ZipOutputStream(bos)) {
                zos.setLevel(config.getCompressionLevel());

                for (var world : worlds) {
                    var worldFolder = world.getWorldFolder().toPath();
                    var rootEntryName = world.getName();
                    appendZip(worldFolder, rootEntryName, zos);
                    world.setAutoSave(true);
                }
            } catch (IOException e) {
                logger.severe("Backup failed: " + e.getMessage());
            } finally {
                plugin.getServer().broadcast(Component.text("Server backup complete", NamedTextColor.GREEN));
                running.set(false);
            }
        }, 20L); // This delay is necessary because otherwise the backup will run before the world save is properly completed, causing the backup-data to be corrupted.
    }

    private void appendZip(@NotNull final Path folder, @NotNull final String rootPath,
                           @NotNull final ZipOutputStream zos) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull final Path file,
                                                      @NotNull final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }

                var relativePath = folder.relativize(file);
                var zipEntryName = rootPath + "/" + relativePath.toString().replace("\\", "/");

                zos.putNextEntry(new ZipEntry(zipEntryName));
                Files.copy(file, zos);
                zos.closeEntry();

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void cleanupBackups(@NotNull final Path folder) {
        try (var files = Files.list(folder)) {
            var zipBackups = files
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            logger.warning("Failed to get modified time for: " + p);
                            return Long.MAX_VALUE; // Push unreadable files to the end
                        }
                    }))
                    .toList();

            int excess = zipBackups.size() - config.getMaxBackups();
            if (excess <= 0) {
                return;
            }

            for (int i = 0; i < excess; i++) {
                var oldBackup = zipBackups.get(i);
                try {
                    Files.deleteIfExists(oldBackup);
                    logger.info("Deleted old backup: " + oldBackup.getFileName());
                } catch (IOException e) {
                    logger.warning("Failed to delete backup '" + oldBackup.getFileName() + "': " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Failed to clean up backups in folder '" + folder + "': " + e.getMessage());
        }
    }

    @Override
    public @NotNull String name() {
        return "backup";
    }
}
