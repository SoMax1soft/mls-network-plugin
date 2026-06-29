package wtf.mlsac.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimedErrorStateTest {

    @Test
    void inactiveByDefault() {
        TimedErrorState state = new TimedErrorState(1000);
        assertFalse(state.isActive());
        assertFalse(state.isExpired(10_000));
    }

    @Test
    void expiresStrictlyAfterTheWindow() {
        TimedErrorState state = new TimedErrorState(1000);
        state.enter(5000);
        assertTrue(state.isActive());
        assertFalse(state.isExpired(5000));
        assertFalse(state.isExpired(6000), "boundary is exclusive (uses > not >=)");
        assertTrue(state.isExpired(6001));
    }

    @Test
    void rawFlagIgnoresExpiryUntilCleared() {
        TimedErrorState state = new TimedErrorState(1000);
        state.enter(5000);
        // Even long past the window, the latched flag stays set until the owner clears it.
        assertTrue(state.isActive());
        assertTrue(state.isExpired(999_999));
        assertTrue(state.isActive());
    }

    @Test
    void clearResetsEverything() {
        TimedErrorState state = new TimedErrorState(1000);
        state.enter(5000);
        state.clear();
        assertFalse(state.isActive());
        assertFalse(state.isExpired(999_999));
    }
}
