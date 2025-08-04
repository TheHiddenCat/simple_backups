package nl.lewin.simpleBackups.compression;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface CompressionStrategy {
    void compress(@NotNull List<Path> sources, @NotNull Path target) throws IOException;
    @NotNull String getFileExtension();
}
