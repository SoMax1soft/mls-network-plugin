// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SharedSightCheckTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(SharedSightCheck.PROPERTY);
    }

    @Test
    void sharedSightLinesStayOffWithoutAVerifiedServer() {
        assertFalse(SharedSightCheck.enabled(null));
        System.setProperty(SharedSightCheck.PROPERTY, "off");
        assertFalse(SharedSightCheck.enabled(null));
    }

    @Test
    void theOwnerCanForceThemOn() {
        System.setProperty(SharedSightCheck.PROPERTY, "on");
        assertTrue(SharedSightCheck.enabled(null));
    }

    @Test
    void parallelWorldTickingTurnsSharedRaysOffWhileItIsOn() {
        String name = ParallelFlag.class.getName();
        java.util.function.BooleanSupplier shared = SharedSightCheck.unlessParallel(getClass().getClassLoader(), name);
        ParallelFlag.enabled = false;
        assertTrue(shared.getAsBoolean());
        ParallelFlag.enabled = true;
        assertFalse(shared.getAsBoolean());
        ParallelFlag.enabled = false;
        assertTrue(SharedSightCheck.unlessParallel(getClass().getClassLoader(), "no.such.Ticking").getAsBoolean());
    }

    public static final class ParallelFlag {
        public static boolean enabled;
    }
}
