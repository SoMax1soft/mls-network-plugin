// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class DiskDataFolder implements DataFolder {

    private final Path root;

    public DiskDataFolder(Path root) {
        this.root = root;
    }

    @Override
    public Optional<String> readText(String relative) throws IOException {
        Path file = resolve(relative);
        return Files.isRegularFile(file) ? Optional.of(Files.readString(file, StandardCharsets.UTF_8)) : Optional.empty();
    }

    @Override
    public void writeText(String relative, String content) throws IOException {
        Path target = resolve(relative);
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public boolean isFile(String relative) {
        return Files.isRegularFile(resolve(relative));
    }

    @Override
    public void move(String from, String to) throws IOException {
        Path target = resolve(to);
        Files.createDirectories(target.getParent());
        Files.move(resolve(from), target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deleteDirectoryIfEmpty(String relative) throws IOException {
        Path directory = resolve(relative);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            if (entries.iterator().hasNext()) {
                return;
            }
        }
        Files.delete(directory);
    }

    @Override
    public void ensureDirectory(String relative) throws IOException {
        Files.createDirectories(resolve(relative));
    }

    @Override
    public Path resolve(String relative) {
        return root.resolve(relative).normalize();
    }
}
