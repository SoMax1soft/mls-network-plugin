// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import java.util.ArrayList;
import java.util.List;

public final class ActionReport {

    private final List<String> tokens = new ArrayList<>();

    public void add(String token) {
        tokens.add(token);
    }

    public List<String> tokens() {
        return List.copyOf(tokens);
    }
}
