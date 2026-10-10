// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPOutputStream;

public final class GzipBody {

    private GzipBody() {
    }

    public static byte[] compress(byte[] plain) {
        ByteArrayOutputStream packed = new ByteArrayOutputStream(64 + plain.length / 2);
        try (GZIPOutputStream gzip = new GZIPOutputStream(packed)) {
            gzip.write(plain);
        } catch (IOException impossible) {
            throw new UncheckedIOException("gzip into memory failed", impossible);
        }
        return packed.toByteArray();
    }
}
