// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnimationTest {

    private final RecordingLog log = new RecordingLog();
    private final Set<String> particles = new HashSet<>(Set.of("FLAME", "SMOKE", "DUST", "WITCH", "BLOCK"));
    private final Set<String> sounds = new HashSet<>(Set.of("BLOCK_PORTAL_AMBIENT", "ENTITY_WITHER_SPAWN",
            "BLOCK_NOTE_BLOCK_PLING", "ENTITY_GENERIC_EXPLODE", "ENTITY_EXPERIENCE_ORB_PICKUP"));
    private boolean modern = true;
    private final EffectRegistry registry = new EffectRegistry() {
        @Override
        public boolean hasParticle(String name) {
            return particles.contains(name);
        }

        @Override
        public boolean hasSound(String name) {
            return sounds.contains(name);
        }

        @Override
        public boolean modernParticleNames() {
            return modern;
        }
    };
    private final RecordingEffects effects = new RecordingEffects();
    private final UUID player = UUID.randomUUID();
    private final Placement origin = new Placement(new Point(10, 64, 10), 90f, 5f);

    private ScriptReader reader(StageExtras extras) {
        return new ScriptReader(new ParticleNames(registry, log), registry, extras, log);
    }

    private AnimationLibrary bundled() {
        AnimationLibrary library = new AnimationLibrary(reader(StageExtras.IGNORED_AS_BEFORE), log);
        library.load(List.of(file("classic_ban.yml"), file("dramatic_ban.yml"), file("rainbow_ban.yml")));
        return library;
    }

    private static AnimationFile file(String name) {
        return new AnimationFile(name, () -> BundledResources.yaml("animations/" + name));
    }

    @Test
    void loadsTheThreeBundledAnimations() {
        AnimationLibrary library = bundled();
        assertEquals(List.of("classic_ban", "dramatic_ban", "rainbow_ban"), library.names());
        assertTrue(library.find("CLASSIC_BAN").isPresent());
        AnimationScript classic = library.find("classic_ban").orElseThrow();
        assertEquals(90, classic.lengthTicks());
        assertTrue(classic.unequipVictim());
        assertEquals(2, classic.effectiveLevitation());
        assertEquals(4, classic.stages().size());
        assertTrue(classic.stages().get(0).trail().isEmpty());
    }

    @Test
    void stageTrailsAreReadOnlyWhenApplied() {
        AnimationLibrary library = new AnimationLibrary(reader(StageExtras.APPLIED), log);
        library.load(List.of(file("classic_ban.yml")));
        assertTrue(library.find("classic_ban").orElseThrow().stages().get(0).trail().isPresent());
    }

    @Test
    void brokenFileIsSkippedWithWarning() {
        AnimationLibrary library = new AnimationLibrary(reader(StageExtras.IGNORED_AS_BEFORE), log);
        int loaded = library.load(List.of(file("classic_ban.yml"), new AnimationFile("broken.yml", () -> {
            throw new IllegalStateException("bad yaml");
        }), new AnimationFile("notes.txt", Map::of)));
        assertEquals(1, loaded);
        assertEquals(List.of("[Animation] Failed to load broken.yml: bad yaml"), log.warnings);
    }

    @Test
    void classicBanTimeline() {
        AnimationScript classic = bundled().find("classic_ban").orElseThrow();
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        assertEquals(AnimationLaunch.STARTED, director.start(player, classic, "ban Steve"));
        assertEquals(List.of("close", "resync", "resync-later", "freeze " + new FreezeSpec(255, 128, 130, 2, 90),
                "strip"), effects.calls.subList(0, 5));
        int ticks = 0;
        while (director.tick(player)) {
            ticks++;
            assertTrue(effects.commands.isEmpty());
        }
        assertEquals(89, ticks);
        assertEquals(List.of("ban Steve"), effects.commands);
        assertFalse(director.animating(player));
        assertEquals(75, effects.teleports.size());
        assertEquals(64.0, effects.teleports.get(0).point().y(), 1e-9);
        assertEquals(68.0, effects.teleports.get(74).point().y(), 1e-9);
        assertEquals(90f, effects.teleports.get(74).yaw());
        assertTrue(effects.droppedIndexes.size() > 20);
        assertEquals(effects.droppedIndexes.size(), effects.droppedIndexes.get(effects.droppedIndexes.size() - 1) + 1);
        assertTrue(effects.droppedIndexes.size() == 36
                || effects.calls.contains("drop-rest " + effects.droppedIndexes.size() + " at " + origin.point()));
        assertTrue(effects.calls.contains("unfreeze"));
    }

    @Test
    void offlineAtStartRunsCommandAtOnce() {
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        AnimationScript classic = bundled().find("classic_ban").orElseThrow();
        assertEquals(AnimationLaunch.RAN_COMMAND_NOW, director.start(player, classic, "ban x"));
        assertEquals(List.of("ban x"), effects.commands);
    }

    @Test
    void secondAnimationWhileAnimatingIsDropped() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        AnimationScript classic = bundled().find("classic_ban").orElseThrow();
        director.start(player, classic, "ban a");
        assertEquals(AnimationLaunch.ALREADY_ANIMATING, director.start(player, classic, "ban b"));
        director.quit(player);
        assertEquals(List.of("ban a"), effects.commands);
    }

    @Test
    void quitMidAnimationDropsItemsAtStartAndRunsCommand() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, bundled().find("classic_ban").orElseThrow(), "ban a");
        for (int i = 0; i < 10; i++) {
            director.tick(player);
        }
        effects.online.remove(player);
        director.quit(player);
        assertEquals(List.of("ban a"), effects.commands);
        assertTrue(effects.calls.stream().anyMatch(call -> call.startsWith("drop-rest") && call.endsWith(origin.point().toString())));
        assertFalse(effects.calls.contains("unfreeze"));
    }

    @Test
    void goingOfflineIsNoticedOnTheNextTick() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, bundled().find("classic_ban").orElseThrow(), "ban a");
        effects.online.remove(player);
        assertFalse(director.tick(player));
        assertEquals(List.of("ban a"), effects.commands);
    }

    @Test
    void errorInsideTickStopsAndRunsCommand() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, bundled().find("classic_ban").orElseThrow(), "ban a");
        effects.failOnSound = new IllegalStateException("sound");
        assertFalse(director.tick(player));
        assertEquals(List.of("ban a"), effects.commands);
        assertEquals(1, log.severes.size());
    }

    @Test
    void abandonAllRunsNoCommands() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, bundled().find("classic_ban").orElseThrow(), "ban a");
        assertEquals(1, director.abandonAll());
        assertTrue(effects.commands.isEmpty());
        assertFalse(director.animating(player));
    }

    @Test
    void sphereEmitsDistinctPointsOnTheLerpedRadius() {
        Point center = new Point(0, 70, 0);
        List<Emission> points = ShapePattern.SPHERE.emit(center, 2.5, 30, 7, () -> 0.5);
        assertEquals(30, points.size());
        assertEquals(30, new HashSet<>(points.stream().map(Emission::at).toList()).size());
        for (Emission emission : points) {
            Point offset = emission.at().plus(center.times(-1));
            assertEquals(2.5, offset.length(), 1e-9);
            assertEquals(1, emission.count());
        }
    }

    @Test
    void otherShapes() {
        Point center = new Point(0, 0, 0);
        assertEquals(List.of(new Emission(center, 4)), ShapePattern.POINT.emit(center, 1, 4, 3, () -> 0.1));
        assertEquals(2, ShapePattern.HELIX.emit(center, 1, 4, 3, () -> 0.1).size());
        assertEquals(8, ShapePattern.CIRCLE.emit(center, 1, 3, 3, () -> 0.1).size());
        assertEquals(12, ShapePattern.CIRCLE.emit(center, 1, 12, 3, () -> 0.1).size());
        List<Emission> explosion = ShapePattern.EXPLOSION.emit(center, 2, 5, 3, () -> 0.9);
        assertEquals(5, explosion.size());
        assertEquals(2.0, explosion.get(0).at().length(), 1e-9);
        assertEquals(5, explosion.get(0).count());
        Point spiral = ShapePattern.RISING_SPIRAL.emit(center, 1, 1, 18, () -> 0).get(0).at();
        assertEquals(Math.cos(Math.toRadians(0)), spiral.x(), 1e-9);
        assertEquals(ShapePattern.NONE, ShapePattern.named("STAR"));
        assertTrue(ShapePattern.named("star").emit(center, 1, 1, 1, () -> 0).isEmpty());
        assertEquals(ShapePattern.HELIX, ShapePattern.named("helix"));
    }

    @Test
    void particleAliasesFollowTheServerGeneration() {
        ParticleNames names = new ParticleNames(registry, log);
        assertEquals("WITCH", names.resolve("SPELL_WITCH"));
        assertEquals("BLOCK", names.resolve("footstep"));
        assertEquals("FLAME", names.resolve("NOT_A_PARTICLE"));
        assertEquals(1, log.warnings.size());
        modern = false;
        particles.clear();
        particles.addAll(Set.of("SPELL_WITCH", "REDSTONE", "BLOCK_CRACK", "FLAME"));
        assertEquals("SPELL_WITCH", names.resolve("WITCH"));
        assertEquals("REDSTONE", names.resolve("DUST"));
        assertEquals("BLOCK_CRACK", names.resolve("BLOCK"));
    }

    @Test
    void dustColourOnlyForColouredDust() {
        assertTrue(ParticleLook.of("DUST", true, 300, -5, 10, 1.2).dust().isPresent());
        assertEquals(new DustColor(255, 0, 10, 1.2), ParticleLook.of("REDSTONE", true, 300, -5, 10, 1.2).dust().get());
        assertTrue(ParticleLook.of("FLAME", true, 1, 1, 1, 1).dust().isEmpty());
        assertTrue(ParticleLook.of("DUST", false, 1, 1, 1, 1).dust().isEmpty());
    }

    @Test
    void readerDefaultsAndUnknownSound() {
        AnimationScript script = reader(StageExtras.APPLIED).read("Custom.yml", Map.of(
                "stages", List.of(Map.of("sounds", List.of(Map.of("soundType", "nope"))),
                        Map.of("titles", List.of(Map.of("title", "Bye")), "lightningTicks", List.of(0, 2)))));
        assertEquals("custom", script.name());
        assertEquals(80, script.lengthTicks());
        assertTrue(script.holdVictimStill());
        assertTrue(script.lightningAtEnd());
        assertFalse(script.unequipVictim());
        assertEquals(1, script.effectiveLevitation());
        assertEquals(Optional.empty(), script.movement());
        assertEquals("Unnamed Stage", script.stages().get(0).name());
        assertEquals(ScriptReader.DEFAULT_SOUND, script.stages().get(0).sounds().get(0).sound());
        assertEquals(1, log.warnings.size());
        reader(StageExtras.APPLIED).read("empty.yml", Map.of());
        assertEquals("[Animation] empty.yml has no stages", log.warnings.get(1));
    }

    @Test
    void stageCuesFollowStageTicks() {
        SoundCue loop = new SoundCue("S", 0, 1, 1, true, 3);
        SoundCue once = new SoundCue("O", 2, 1, 1, false, 20);
        ScriptStage stage = new ScriptStage("s", 2, 8, List.of(), List.of(loop, once),
                List.of(new TitleCue(1, "T", "", 1, 1, 1)), List.of(4), Optional.empty());
        AnimationScript script = new AnimationScript("x", 10, false, true, false, 5, -1, Optional.empty(), List.of(stage));
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, script, "cmd");
        while (director.tick(player)) {
            effects.calls.add("tick");
        }
        List<String> interesting = effects.calls.stream().filter(call -> !call.startsWith("resync")
                && !call.equals("close")).toList();
        assertEquals(List.of("tick", "sound S", "tick", "title T", "tick", "sound O", "tick", "sound S", "tick",
                "lightning", "tick", "tick", "sound S", "tick", "tick", "lightning"), interesting);
        assertEquals(List.of("cmd"), effects.commands);
    }

    @Test
    void movementWithoutSmoothingTeleportsOnce() {
        MovementPath path = new MovementPath(3, 6, 0, 2, 0, false);
        assertEquals(Optional.of(new Point(0, 2, 0)), path.target(3, new Point(0, 0, 0)));
        assertEquals(Optional.empty(), path.target(4, new Point(0, 0, 0)));
        assertEquals(new Point(1, 5, 3), MovementClamp.verticalOnly(new Point(1, 2, 3), new Point(9, 5, 9)));
    }
}
