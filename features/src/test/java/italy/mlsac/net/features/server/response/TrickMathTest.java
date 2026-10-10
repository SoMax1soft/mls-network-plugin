// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.testkit.ScriptedRandom;
import java.util.List;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class TrickMathTest {

    @Test
    void aThrownWeaponFliesAlongTheLookWithTheGivenLift() {
        Vector push = ThrowVelocity.along(new Vector(0.6, -0.8, 0), 1.4, 0.45);
        assertEquals(0.84, push.getX(), 1e-9);
        assertEquals(0.45, push.getY(), 1e-9);
        assertEquals(0.0, push.getZ(), 1e-9);
    }

    @Test
    void aLaunchPushesAgainstTheLook() {
        Vector push = ThrowVelocity.against(new Vector(0, 0.3, 1), 2.0, 0.6);
        assertEquals(0.0, push.getX(), 1e-9);
        assertEquals(0.6, push.getY(), 1e-9);
        assertEquals(-2.0, push.getZ(), 1e-9);
    }

    @Test
    void theShuffleIsAPermutationDrivenByTheRandomSource() {
        ScriptedRandom random = new ScriptedRandom(0);
        String[] slots = {"a", "b", "c", "d"};
        new ShuffleOrder(random).shuffle(slots);
        assertArrayEquals(new String[] {"b", "c", "d", "a"}, slots);
        assertEquals(List.of(4L, 3L, 2L), random.bounds());
    }
}
