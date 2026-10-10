// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.message;

import java.util.List;

public sealed interface MessageOverride permits MessageOverride.Text, MessageOverride.Lines {

    record Text(String value) implements MessageOverride {
    }

    record Lines(List<String> values) implements MessageOverride {

        public Lines {
            values = List.copyOf(values);
        }
    }
}
