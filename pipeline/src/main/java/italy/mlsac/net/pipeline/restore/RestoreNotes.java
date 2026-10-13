// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import java.util.function.Supplier;

public interface RestoreNotes {

    RestoreNotes SILENT = new RestoreNotes() {
        @Override
        public void note(Supplier<String> line) {
        }

        @Override
        public void trouble(String line) {
        }
    };

    void note(Supplier<String> line);

    void trouble(String line);
}
