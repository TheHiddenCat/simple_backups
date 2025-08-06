package nl.lewin.simpleBackups;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class PluginConfig {
    private final int compressionLevel;
    private final int maxBackups;

    public PluginConfig(@NotNull final FileConfiguration config) {
        this.compressionLevel = config.getInt("compression-level", 9);
        this.maxBackups = config.getInt("max-backups", 5);
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public int getMaxBackups() {
        return maxBackups;
    }
}
