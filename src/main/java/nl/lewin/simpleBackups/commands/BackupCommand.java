package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupCommand implements SubCommand {
    private static final @NotNull AtomicBoolean running = new AtomicBoolean(false);
    private final @NotNull Plugin plugin;
    private final @NotNull Logger logger;

    public BackupCommand(@NotNull final Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
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

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try (var zos = new ZipOutputStream(Files.newOutputStream(backupZip, StandardOpenOption.WRITE))) {
                zos.setLevel(Deflater.BEST_COMPRESSION);

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
        }, 20L);
    }

    private void appendZip(@NotNull final Path folder, @NotNull final String rootPath, @NotNull final ZipOutputStream zos) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull final Path file, @NotNull final BasicFileAttributes attrs) throws IOException {
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

    @Override
    public @NotNull String name() {
        return "backup";
    }
}
