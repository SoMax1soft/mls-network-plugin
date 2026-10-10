// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import java.util.ArrayDeque;
import java.util.Deque;

final class ShutdownSteps {

    private final Deque<Runnable> steps = new ArrayDeque<>();

    void add(Runnable step) {
        steps.push(step);
    }

    void runAll() {
        while (!steps.isEmpty()) {
            steps.pop().run();
        }
    }
}
