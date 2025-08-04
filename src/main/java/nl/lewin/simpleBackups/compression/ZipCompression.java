package nl.lewin.simpleBackups.compression;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipCompression implements CompressionStrategy {
    private final @NotNull Logger logger;

    public ZipCompression(@NotNull final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void compress(@NotNull final List<Path> sources, @NotNull final Path target) throws IOException {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("No source paths provided for compression.");
        }

        try (var zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            zos.setLevel(Deflater.BEST_COMPRESSION);
            for (var source : sources) {
                compressSource(source, zos);
            }
        } catch (IOException openEx) {
            logger.severe("Failed to open archive for writing: " + target);
            throw openEx;
        }
    }

    private void compressSource(@NotNull final Path source, @NotNull final ZipOutputStream zos) {
        if (!Files.exists(source)) {
            logger.warning("Skipping missing source: " + source);
            return;
        }

        var baseDir = source.getParent();
        if (baseDir == null) {
            logger.warning("Cannot determine base directory for: " + source);
            return;
        }

        try (var stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> addFileToZip(path, baseDir, zos));
        } catch (IOException walkEx) {
            logger.severe("Failed to walk directory: " + source);
        }
    }

    private void addFileToZip(@NotNull final Path filePath, @NotNull final Path baseDir,
                              @NotNull final ZipOutputStream zos) {
        var relativePath = baseDir.relativize(filePath);
        var zipEntryPath = relativePath.toString().replace("\\", "/"); // For Windows

        try {
            var entry = new ZipEntry(zipEntryPath);
            zos.putNextEntry(entry);

            try (var in = Files.newInputStream(filePath)) {
                final var buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
            }

            zos.closeEntry();
        } catch (IOException fileEx) {
            logger.severe("Failed to compress file: " + filePath);
        }
    }

    @Override
    public @NotNull String getFileExtension() {
        return ".zip";
    }
}