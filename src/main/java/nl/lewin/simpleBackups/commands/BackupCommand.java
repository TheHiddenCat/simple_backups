package nl.lewin.simpleBackups.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        plugin.getServer().broadcast(Component.text("[!] Server backup has started", NamedTextColor.YELLOW));

        var worlds = Bukkit.getWorlds();
        for(var world : worlds) {
            world.save();
            world.setAutoSave(false);
            logger.info("Saved world: " + world.getName());
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            player.saveData();
            logger.info("Saved player with UUID: " + player.getUniqueId());
        }

        var backupFolder = new File(plugin.getDataFolder(), "backups");
        var fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()) + ".zip";
        var backupZip = new File(backupFolder, fileName);
        try {
            backupFolder.mkdirs();
            backupZip.createNewFile();
        } catch (IOException e) {
            logger.severe("Failed to create essential directories for backup");
            return;
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try (var zos = new ZipOutputStream(new FileOutputStream(backupZip))) {
                zos.setLevel(9);
                for (var world : worlds) {
                    var worldFolder = world.getWorldFolder();
                    appendZip(worldFolder, world.getName(), zos);
                    world.setAutoSave(true);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            plugin.getServer().broadcast(Component.text("Server backup complete", NamedTextColor.GREEN));
        }, 20L);
    }

    private void appendZip(@NotNull final File folder, @NotNull final String path, @NotNull final ZipOutputStream zos) {
        var files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (var file : files) {
            var filePath = path + "/" + file.getName();
            if (file.isDirectory()) {
                appendZip(file, filePath, zos);
            }
            else {
                // Skip "session.lock" because it can cause issues
                if(file.getName().equals("session.lock")) {
                    continue;
                }
                try(var bis = new BufferedInputStream(new FileInputStream(file))) {
                    zos.putNextEntry(new ZipEntry(filePath));
                    var buffer = new byte[16384];
                    int length;
                    while ((length = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    logger.severe("Failed to append to zip: " + path);
                }
            }
        }
    }

    @Override
    public @NotNull String name() {
        return "backup";
    }
}
