// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.PlatformText;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiskFiles implements DataFolder, PlatformText {

    private final italy.mlsac.net.storage.DataFolder root;

    public DiskFiles(italy.mlsac.net.storage.DataFolder root) {
        this.root = root;
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(root.resolve(relativePath));
    }

    @Override
    public void write(String relativePath, byte[] content) throws IOException {
        Path target = root.resolve(relativePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, content);
    }

    @Override
    public String lineSeparator() {
        return System.lineSeparator();
    }

    @Override
    public Charset defaultCharset() {
        return Charset.defaultCharset();
    }
}
