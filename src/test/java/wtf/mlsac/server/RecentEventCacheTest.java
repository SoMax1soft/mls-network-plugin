package wtf.mlsac.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentEventCacheTest {

    @Test
    void firstSightingIsNewSubsequentIsNot() {
        RecentEventCache cache = new RecentEventCache(8);
        assertTrue(cache.markIfNew("a"));
        assertFalse(cache.markIfNew("a"));
    }

    @Test
    void blankIdsAreNeverDeduplicated() {
        RecentEventCache cache = new RecentEventCache(8);
        assertTrue(cache.markIfNew(null));
        assertTrue(cache.markIfNew(null));
        assertTrue(cache.markIfNew(""));
        assertTrue(cache.markIfNew(""));
    }

    @Test
    void evictsOldestOnceCapacityExceeded() {
        RecentEventCache cache = new RecentEventCache(2);
        assertTrue(cache.markIfNew("a"));
        assertTrue(cache.markIfNew("b"));
        // "c" pushes the cache past capacity 2, evicting the oldest ("a").
        assertTrue(cache.markIfNew("c"));
        // "a" was evicted, so it now looks new again.
        assertTrue(cache.markIfNew("a"));
        // "b"/"c" are still remembered.
        assertFalse(cache.markIfNew("c"));
    }

    @Test
    void clearForgetsEverything() {
        RecentEventCache cache = new RecentEventCache(8);
        cache.markIfNew("a");
        cache.clear();
        assertTrue(cache.markIfNew("a"));
    }
}
