// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface DataFolder {

    Optional<String> readText(String relative) throws IOException;

    void writeText(String relative, String content) throws IOException;

    boolean isFile(String relative);

    void move(String from, String to) throws IOException;

    void deleteDirectoryIfEmpty(String relative) throws IOException;

    void ensureDirectory(String relative) throws IOException;

    Path resolve(String relative);
}
