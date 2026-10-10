// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.wire.UltraPayload;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.occlusion.LiveOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.occlusion.WallCheckRequest;
import italy.mlsac.net.pipeline.occlusion.OcclusionKind;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FightAssemblerTest {
    private static final int EXPOSURE_FEATURE = 32;
    private static final int WALL_FLAG = 6;

    @Test
    void shortStretchEndedByNonCombatRowEmitsNothing() {
        List<WindowMatrix> windows = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(windows::add);
        long index = 0;
        for (; index < 59; index++) {
            assembler.tickRow(TickRowFixture.at(index).hits(1).build());
        }
        assembler.tickRow(TickRowFixture.at(index++).withoutPacket().hits(1).build());
        for (int extra = 0; extra < 3; extra++) {
            assembler.tickRow(TickRowFixture.at(index++).outsideFight().build());
        }
        assertTrue(windows.isEmpty());
    }

    @Test
    void cutWithoutRecentHitIsSkippedNotDelayed() {
        List<WindowMatrix> windows = new ArrayList<>();
        List<Long> emittedAt = new ArrayList<>();
        long[] handed = {0};
        FightAssembler assembler = new FightAssembler(window -> {
            windows.add(window);
            emittedAt.add(handed[0]);
        });
        for (long index = 0; index < 92; index++) {
            handed[0] = index;
            int hits = index == 29 || index == 70 ? 1 : 0;
            assembler.tickRow(TickRowFixture.at(index).hits(hits).build());
        }
        assertEquals(1, windows.size());
        assertEquals(List.of(91L), emittedAt);
    }

    @Test
    void windowUsesOcclusionValuesAsTheyAreWhenBuilt() {
        List<WindowMatrix> windows = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(windows::add);
        LiveOcclusion occlusion = new LiveOcclusion(new OcclusionProbe((world, from, to) -> RayVerdict.CLEAR));
        PlayerSample eye = new PlayerSample(new UUID(1, 1), new UUID(2, 2), 1, 0L, 0.0, 64.0, 0.0, 0f, 0f, 1.62, 1.8,
                0.6, 0.0, 1f, true, false, PlayerPosture.NEUTRAL, "air", null, null, null);
        for (long index = 0; index < 62; index++) {
            OcclusionCell cell = new OcclusionCell();
            TickRow row = TickRowFixture.at(index).hits(1).occlusion(cell).build();
            if (index < 57) {
                occlusion.resolveTick(WallCheckRequest.of(OcclusionKind.TICK, cell, eye, null, TickRowFixture.TIGHT,
                        TickRowFixture.TIGHT, 0f, 0f));
            }
            assembler.tickRow(row);
        }
        assertEquals(1, windows.size());
        WindowMatrix window = windows.get(0);
        assertEquals(1.0f, window.features()[56][EXPOSURE_FEATURE]);
        assertEquals(1.0f, window.flags()[56][WALL_FLAG]);
        assertEquals(0.0f, window.features()[57][EXPOSURE_FEATURE]);
        assertEquals(0.0f, window.flags()[57][WALL_FLAG]);
        assertEquals(0.0f, window.flags()[59][WALL_FLAG]);
    }

    @Test
    void encodesVersionFourWithTheDocumentedHeader() {
        List<WindowMatrix> windows = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(windows::add);
        for (long index = 0; index < 62; index++) {
            assembler.tickRow(TickRowFixture.at(index).hits(1).build());
        }
        byte[] bytes = MlsuWire.encode(WireVersion.V4, windows.get(0).narrowedTo(WireVersion.V4));
        assertEquals(15_380, bytes.length);
        assertEquals("4d4c5355000000040000003c000000320000000e", hex(bytes, 20));
        assertEquals(new MlsuWire.MlsuHeader(4, 60, 50, 14, 15_380), MlsuWire.header(bytes));
        byte[] narrow = MlsuWire.encode(WireVersion.V2, windows.get(0).narrowedTo(WireVersion.V2));
        assertEquals(WireVersion.V2.byteLength(60), narrow.length);
    }

    @Test
    void wideWindowNarrowsToTheExactVersionFourBytes() {
        List<WindowMatrix> windows = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(windows::add);
        for (long index = 0; index < 62; index++) {
            assembler.tickRow(TickRowFixture.at(index).hits(1).build());
        }
        byte[] wide = MlsuWire.encode(WireVersion.V6, windows.get(0));
        assertEquals(new MlsuWire.MlsuHeader(6, 60, 54, 18, 17_300), MlsuWire.header(wide));
        byte[] four = MlsuWire.encode(WireVersion.V4, windows.get(0).narrowedTo(WireVersion.V4));
        assertArrayEquals(four, UltraPayload.read(wide).orElseThrow().layoutFour());
        assertArrayEquals(four, UltraPayload.narrowed(new WindowSubmission(new UUID(3, 3), "p", wide)).window());
    }

    private static String hex(byte[] bytes, int count) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < count; index++) {
            text.append(String.format("%02x", bytes[index]));
        }
        return text.toString();
    }
}
