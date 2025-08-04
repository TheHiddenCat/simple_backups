package nl.lewin.simpleBackups.tasks;

import nl.lewin.simpleBackups.compression.CompressionStrategy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class BackupTask extends BukkitRunnable {
    private final @NotNull Logger logger;
    private final @NotNull File backupDir;
    private final @NotNull CompressionStrategy compression;
    private final @NotNull Runnable onFinish;

    public BackupTask(@NotNull final Logger logger, @NotNull final File backupDir,
                      @NotNull final CompressionStrategy compression, @NotNull final Runnable onFinish) {
        this.backupDir = backupDir;
        this.compression = compression;
        this.onFinish = onFinish;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            backupWorlds();
        } catch (IOException e) {
            logger.severe("Failed to backup worlds");
        }
    }

    private void backupWorlds() throws IOException {
        var worldFolders = new ArrayList<Path>();
        for (var world : Bukkit.getWorlds()) {
            var path = world.getWorldFolder().toPath();
            if (Files.exists(path)) {
                worldFolders.add(path);
            }
        }

        if (worldFolders.isEmpty()) {
            logger.warning("No world folders found to back up.");
            return;
        }

        var fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(ZonedDateTime.now()) + compression.getFileExtension();
        var backupFile = backupDir.toPath().resolve(fileName);
        logger.info("Creating backup: " + backupFile);
        compression.compress(worldFolders, backupFile);
        logger.info("Backup complete: " + fileName);

        onFinish.run();
    }
}
