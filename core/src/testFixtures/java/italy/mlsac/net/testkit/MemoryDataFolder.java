// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.storage.DataFolder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class MemoryDataFolder implements DataFolder {

    private final Map<String, String> files = new TreeMap<>();
    private final Set<String> directories = new TreeSet<>();

    @Override
    public synchronized Optional<String> readText(String relative) {
        return Optional.ofNullable(files.get(relative));
    }

    @Override
    public synchronized void writeText(String relative, String content) {
        files.put(relative, content);
    }

    @Override
    public synchronized boolean isFile(String relative) {
        return files.containsKey(relative);
    }

    @Override
    public synchronized void move(String from, String to) throws IOException {
        String content = files.remove(from);
        if (content == null) {
            throw new IOException("no such file " + from);
        }
        files.put(to, content);
    }

    @Override
    public synchronized void deleteDirectoryIfEmpty(String relative) {
        boolean hasChildren = files.keySet().stream().anyMatch(name -> name.startsWith(relative + "/"));
        if (!hasChildren) {
            directories.remove(relative);
        }
    }

    @Override
    public synchronized void ensureDirectory(String relative) {
        directories.add(relative);
    }

    @Override
    public Path resolve(String relative) {
        return Path.of("memory").resolve(relative);
    }

    public synchronized Map<String, String> files() {
        return Map.copyOf(files);
    }

    public synchronized Set<String> directories() {
        return Set.copyOf(directories);
    }
}
