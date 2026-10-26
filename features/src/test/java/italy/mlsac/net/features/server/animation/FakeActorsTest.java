// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.banimation.ActorGesture;
import italy.mlsac.net.banimation.ActorKind;
import italy.mlsac.net.banimation.ActorPose;
import italy.mlsac.net.banimation.AnimationScript;
import italy.mlsac.net.banimation.EffectRegistry;
import italy.mlsac.net.banimation.ParticleNames;
import italy.mlsac.net.banimation.Placement;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.banimation.ScriptReader;
import italy.mlsac.net.banimation.StageExtras;
import italy.mlsac.net.packet.MetadataEntry;
import italy.mlsac.net.packet.VirtualPose;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.List;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

class FakeActorsTest {

    private static final UUID VICTIM = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-0000000a11ce");
    private static final UUID WATCHER = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-0000000b0b00");

    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    private final FakeActors actors = new FakeActors(packets.sender());

    @Test
    void wardenLivesOnlyInPacketsToItsViewers() {
        actors.spawn(VICTIM, ActorKind.WARDEN, new Placement(new Point(12.5, 64, 10), 90f, 0f), ActorPose.EMERGING,
                List.of(VICTIM, WATCHER));
        actors.pose(VICTIM, ActorPose.ROARING);
        actors.move(VICTIM, new Placement(new Point(12.0, 70, 10), 90f, 0f));
        actors.gesture(VICTIM, ActorGesture.ATTACK);
        actors.gesture(VICTIM, ActorGesture.SHAKE_TENDRILS);
        actors.remove(VICTIM);
        actors.remove(VICTIM);
        actors.pose(VICTIM, ActorPose.DIGGING);

        int id = FakeActors.FIRST_ID;
        List<String> toWatcher = packets.sent().stream().filter(line -> line.contains(WATCHER.toString())).toList();
        List<String> written = toWatcher.stream().filter(line -> !line.startsWith("flush")).toList();
        assertEquals(7, written.size());
        assertEquals(6, toWatcher.size() - written.size());
        assertTrue(written.get(0).startsWith("spawn " + WATCHER + " VirtualEntity[entityNumber=" + id));
        assertTrue(written.get(0).contains("type=WARDEN, x=12.5, y=64.0, z=10.0]"));
        assertTrue(written.get(0).endsWith(" yaw 90.0"));
        assertEquals("metadata " + WATCHER + " " + id + " "
                + List.of(new MetadataEntry.PoseValue(6, VirtualPose.EMERGING)), written.get(1));
        assertEquals("metadata " + WATCHER + " " + id + " "
                + List.of(new MetadataEntry.PoseValue(6, VirtualPose.ROARING)), written.get(2));
        assertEquals("move " + WATCHER + " " + id + " -0.5 0.0 0.0 yaw 90.0", written.get(3));
        assertEquals("event " + WATCHER + " " + id + " 4", written.get(4));
        assertEquals("event " + WATCHER + " " + id + " 61", written.get(5));
        assertEquals("destroy " + WATCHER + " [" + id + "]", written.get(6));
        assertEquals(2 * toWatcher.size(), packets.sent().size());
    }

    @Test
    void everyVictimGetsItsOwnWardenAndRespawnReplacesTheOld() {
        UUID other = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-0000000c0c00");
        Placement at = new Placement(new Point(0, 64, 0), 0f, 0f);
        actors.spawn(VICTIM, ActorKind.WARDEN, at, ActorPose.EMERGING, List.of(WATCHER));
        actors.spawn(other, ActorKind.WARDEN, at, ActorPose.EMERGING, List.of(WATCHER));
        actors.spawn(VICTIM, ActorKind.WARDEN, at, ActorPose.STANDING, List.of(WATCHER));
        List<String> destroyed = packets.sent().stream().filter(line -> line.startsWith("destroy")).toList();
        assertEquals(List.of("destroy " + WATCHER + " [" + FakeActors.FIRST_ID + "]"), destroyed);
        assertTrue(packets.sent().stream().anyMatch(line -> line.contains("entityNumber=" + (FakeActors.FIRST_ID - 2))));
    }

    @Test
    void wardenScriptNamesExistOnThePaperApi() {
        RecordingLog log = new RecordingLog();
        EffectRegistry paper = new EffectRegistry() {
            @Override
            public boolean hasParticle(String name) {
                return constant(Particle.class, name);
            }

            @Override
            public boolean hasSound(String name) {
                return constant(Sound.class, name);
            }

            @Override
            public boolean modernParticleNames() {
                return constant(Particle.class, "DUST");
            }

            @Override
            public boolean hasActor(ActorKind kind) {
                return true;
            }
        };
        AnimationScript warden = new ScriptReader(new ParticleNames(paper, log), paper, StageExtras.IGNORED_AS_BEFORE,
                log).read("warden_ban.yml", BundledResources.yaml("animations/warden_ban.yml"));
        assertEquals(List.of(), log.warnings);
        assertTrue(warden.actor().isPresent());
    }

    private static boolean constant(Class<?> owner, String name) {
        try {
            owner.getField(name);
            return true;
        } catch (NoSuchFieldException missing) {
            return false;
        }
    }
}
