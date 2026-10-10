// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViewerTickTest {
    private static final UUID WORLD = new UUID(9, 9);
    private static final UUID FIRST = new UUID(1, 1);
    private static final UUID SECOND = new UUID(2, 2);

    @Test
    void viewerTickProbesOnlyThatViewer() {
        List<UUID> probed = new ArrayList<>();
        CompensationBook book = new CompensationBook((viewer, id) -> probed.add(viewer), purpose -> 0L);
        book.track(FIRST, new ViewSeed("V_1_21_4", WORLD, new Vec3(0, 64, 0), List.of()));
        book.track(SECOND, new ViewSeed("V_1_21_4", WORLD, new Vec3(5, 64, 0), List.of()));
        book.endViewerTick(SECOND);
        assertEquals(List.of(SECOND), probed);
        book.endViewerTick(new UUID(7, 7));
        assertEquals(1, probed.size());
        book.endServerTick();
        assertEquals(3, probed.size());
    }
}
