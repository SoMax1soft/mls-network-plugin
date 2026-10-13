// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.detection.RestoreAnswer;
import italy.mlsac.net.detection.RestoreLabel;
import italy.mlsac.net.detection.RestoreOutcome;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.pipeline.engine.JointTapSource;
import italy.mlsac.net.pipeline.restore.RestoreDesk;
import italy.mlsac.net.pipeline.restore.RestoreLimits;
import italy.mlsac.net.pipeline.restore.RestoreNotes;
import italy.mlsac.net.pipeline.restore.RestoreTaps;
import italy.mlsac.net.pipeline.tables.SampleHeaders;
import italy.mlsac.net.pipeline.tables.TableColumns;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestoreBuffersTest {
    private static final RestoreLimits ROOMY = new RestoreLimits(true, 40, 64L * 1024 * 1024);

    private RestoreLimits limits = ROOMY;
    private final ArenaServer server = new ArenaServer();
    private final ArenaFight fight = new ArenaFight(server, 4);
    private final RestoreDesk desk = new RestoreDesk(() -> limits, RestoreNotes.SILENT);
    private final PlayerOrderedInput input = new PlayerOrderedInput(
            new DataPipeline(server.ports(), new JointTapSource(InputTapSource.NONE, desk)));

    private void play(long fromTick, long toTick) {
        for (long tick = fromTick; tick <= toTick; tick++) {
            fight.tickServer(input, tick);
            for (int index = 0; index < fight.players().size(); index++) {
                fight.tickClient(input, index, tick);
            }
        }
    }

    private RestoreTaps taps() {
        return desk.taps().orElseThrow();
    }

    private String text(String path) {
        return new String(server.files().get(path), StandardCharsets.UTF_8);
    }

    @Test
    void switchedOffAllocatesNothing() {
        limits = RestoreLimits.OFF;
        input.enabled();
        play(1, 200);
        assertEquals(0, taps().bufferedPlayers());
        assertEquals(0, taps().usedBytes());
        assertFalse(taps().anyActive());
        assertEquals(RestoreOutcome.DISABLED, desk.restore(new RestoreRequest(fight.players().get(0),
                RestoreLabel.CHEAT, "Mod")).outcome());
        assertTrue(server.files().isEmpty());
    }

    @Test
    void fightersAreBufferedAndFreedWhenTheyLeave() {
        input.enabled();
        play(1, 200);
        assertEquals(4, taps().bufferedPlayers());
        long withAll = taps().usedBytes();
        assertTrue(withAll > 0);
        UUID leaving = fight.players().get(0);
        server.disconnect(leaving);
        input.quit(leaving);
        assertFalse(taps().buffering(leaving));
        assertEquals(3, taps().bufferedPlayers());
        assertTrue(taps().usedBytes() < withAll);
        for (UUID player : fight.players()) {
            server.disconnect(player);
            input.quit(player);
        }
        assertEquals(0, taps().bufferedPlayers());
        assertEquals(0, taps().usedBytes());
        assertEquals(RestoreOutcome.NOTHING_BUFFERED, desk.restore(new RestoreRequest(leaving, RestoreLabel.LEGIT,
                "Mod")).outcome());
    }

    @Test
    void switchingOffInThePresetDropsEveryBuffer() {
        input.enabled();
        play(1, 100);
        assertEquals(4, taps().bufferedPlayers());
        limits = RestoreLimits.OFF;
        play(101, 102);
        assertEquals(0, taps().bufferedPlayers());
        assertEquals(0, taps().usedBytes());
    }

    @Test
    void playerAndMemoryCapsHold() {
        limits = new RestoreLimits(true, 2, 64L * 1024 * 1024);
        input.enabled();
        play(1, 200);
        assertEquals(2, taps().bufferedPlayers());
        long cap = 400_000;
        limits = new RestoreLimits(true, 40, cap);
        for (long tick = 201; tick <= 1500; tick++) {
            play(tick, tick);
            assertTrue(taps().usedBytes() <= cap, "used " + taps().usedBytes());
        }
        assertTrue(taps().bufferedPlayers() >= 1);
    }

    @Test
    void restoreWritesTheRecorderFilesAndStartsOver() {
        input.enabled();
        play(1, 300);
        UUID player = fight.players().get(0);
        long othersBefore = taps().usedBytes();
        RestoreAnswer answer = desk.restore(new RestoreRequest(player, RestoreLabel.CHEAT, "Mod"));
        assertEquals(RestoreOutcome.SAVED, answer.outcome());
        assertTrue(answer.ticks() > 200, "ticks " + answer.ticks());
        assertTrue(answer.hits() > 10, "hits " + answer.hits());
        assertTrue(answer.folder().startsWith("data/restore/cheat/CHEAT_Fighter4000_1789000000000_"));
        Map<String, byte[]> files = server.files();
        assertEquals(5, files.size());
        assertEquals(SampleHeaders.SAMPLES + "\n", text(answer.folder() + "/samples.csv"));
        assertEquals(SampleHeaders.ATTACKS + "\n", text(answer.folder() + "/attacks.csv"));
        List<String> ticks = text(answer.folder() + "/ticks.csv").lines().toList();
        assertEquals(TableColumns.TICK_HEADER, ticks.get(0));
        assertEquals(answer.ticks() + 1, ticks.size());
        assertTrue(ticks.get(1).startsWith("1,0,0,"), ticks.get(1));
        List<String> hits = text(answer.folder() + "/hits.csv").lines().toList();
        assertEquals(TableColumns.CLICK_HEADER, hits.get(0));
        assertEquals(answer.hits() + 1, hits.size());
        String session = text(answer.folder() + "/session.json");
        assertTrue(session.contains("\"label\": \"CHEAT\""));
        assertTrue(session.contains("\"comment\": \"restore_cheat\""));
        assertTrue(session.contains("\"restored_by\": \"Mod\""));
        assertTrue(session.contains("\"v2_ticks\": " + answer.ticks()));
        assertFalse(taps().buffering(player));
        assertTrue(taps().usedBytes() < othersBefore);
        play(301, 320);
        assertTrue(taps().buffering(player));
    }
}
