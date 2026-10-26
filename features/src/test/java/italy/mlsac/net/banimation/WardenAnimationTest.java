// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class WardenAnimationTest {

    private static final List<String> SHIPPED = List.of("classic_ban.yml", "dramatic_ban.yml", "rainbow_ban.yml",
            "warden_ban.yml");

    private final RecordingLog log = new RecordingLog();
    private final Set<String> particles = Set.of("FLAME", "SMOKE", "DUST", "LARGE_SMOKE", "FIREWORK",
            "SCULK_SOUL", "SCULK_CHARGE_POP");
    private final Set<String> sounds = Set.of("BLOCK_PORTAL_AMBIENT", "ENTITY_WITHER_SPAWN", "BLOCK_GLASS_BREAK",
            "ENTITY_GENERIC_EXPLODE", "ENTITY_EXPERIENCE_ORB_PICKUP", "BLOCK_NOTE_BLOCK_PLING",
            "BLOCK_SCULK_SHRIEKER_SHRIEK", "BLOCK_SCULK_SPREAD", "BLOCK_SCULK_CATALYST_BLOOM", "ENTITY_WARDEN_EMERGE", "ENTITY_WARDEN_SNIFF",
            "ENTITY_WARDEN_TENDRIL_CLICKS", "ENTITY_WARDEN_ROAR", "ENTITY_WARDEN_ANGRY",
            "ENTITY_WARDEN_ATTACK_IMPACT", "ENTITY_PLAYER_HURT", "ENTITY_WARDEN_DIG");
    private boolean wardens = true;
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
            return true;
        }

        @Override
        public boolean hasActor(ActorKind kind) {
            return wardens;
        }
    };
    private final RecordingEffects effects = new RecordingEffects();
    private final UUID player = UUID.randomUUID();
    private final Placement origin = new Placement(new Point(10, 64, 10), 90f, 5f);

    private ScriptReader reader() {
        return new ScriptReader(new ParticleNames(registry, log), registry, StageExtras.IGNORED_AS_BEFORE, log);
    }

    private AnimationLibrary library() {
        AnimationLibrary library = new AnimationLibrary(reader(), log);
        library.load(SHIPPED.stream().map(WardenAnimationTest::file).toList());
        return library;
    }

    private static AnimationFile file(String name) {
        return new AnimationFile(name, () -> BundledResources.yaml("animations/" + name));
    }

    private AnimationScript warden() {
        return library().find("warden_ban").orElseThrow();
    }

    @Test
    void wardenScriptLoadsWithItsActorAndNoWarnings() {
        AnimationLibrary library = library();
        assertEquals(List.of("classic_ban", "dramatic_ban", "rainbow_ban", "warden_ban"), library.names());
        AnimationScript warden = library.find("WARDEN_BAN").orElseThrow();
        assertEquals("warden_ban", warden.name());
        assertEquals(344, warden.lengthTicks());
        assertEquals(234, warden.punishAtTick());
        assertEquals(Optional.of(new ActorSpec(ActorKind.WARDEN, 2.5)), warden.actor());
        assertFalse(warden.levitates());
        assertFalse(warden.lightningAtEnd());
        assertTrue(warden.unequipVictim());
        assertEquals(0, FreezeSpec.of(warden).levitationTicks());
        assertEquals(384, FreezeSpec.of(warden).slowTicks());
        assertEquals(7, warden.stages().size());
        assertTrue(warden.stages().stream().allMatch(stage -> stage.titles().isEmpty()));
        assertEquals(List.of(), log.warnings);
    }

    @Test
    void oldScriptsKeepLevitatingAndHaveNoActor() {
        AnimationScript classic = library().find("classic_ban").orElseThrow();
        assertTrue(classic.levitates());
        assertTrue(classic.actor().isEmpty());
        assertEquals(90, FreezeSpec.of(classic).levitationTicks());
        assertEquals(0, classic.punishAtTick());
        assertTrue(classic.stages().get(0).actorCues().isEmpty());
        assertEquals(Anchor.VICTIM, classic.stages().get(0).particles().get(0).anchor());
    }

    @Test
    void wardenTimeline() {
        effects.online.put(player, origin);
        List<WorldSpot> detachedAt = new ArrayList<>();
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log, new AnimationListener() {
            @Override
            public void started(UUID who, BooleanSupplier advance) {
            }

            @Override
            public void detached(UUID who, WorldSpot at, BooleanSupplier advance) {
                detachedAt.add(at);
            }

            @Override
            public void ended(UUID who) {
            }
        });
        assertEquals(AnimationLaunch.STARTED, director.start(player, warden(), "ban Steve"));
        assertTrue(effects.calls.contains("freeze " + new FreezeSpec(255, 128, 384, 1, 0)));
        int ticks = 0;
        while (effects.commands.isEmpty()) {
            assertTrue(director.tick(player));
            ticks++;
        }
        assertEquals(234, ticks);
        assertEquals(List.of(new WorldSpot(RecordingEffects.WORLD, origin.point())), detachedAt);
        assertTrue(effects.calls.contains("drop-rest " + effects.droppedIndexes.size() + " at " + origin.point()));
        int dropsAtBite = effects.droppedIndexes.size();

        effects.online.remove(player);
        director.quit(player);
        assertTrue(director.animating(player));
        while (director.tick(player)) {
            ticks++;
        }
        assertEquals(343, ticks);
        assertEquals(List.of("ban Steve"), effects.commands);
        assertFalse(director.animating(player));
        assertEquals(dropsAtBite, effects.droppedIndexes.size());
        assertFalse(effects.calls.contains("unfreeze"));
        assertTrue(effects.calls.stream().noneMatch(call -> call.startsWith("title")));

        List<String> actor = effects.calls.stream().filter(call -> call.startsWith("actor") || call.startsWith("hide"))
                .toList();
        assertEquals(List.of("actor-spawn WARDEN EMERGING", "actor-gesture SHAKE_TENDRILS",
                "actor-gesture SHAKE_TENDRILS", "actor-pose ROARING", "actor-gesture ATTACK", "hide 150",
                "actor-pose DIGGING", "actor-remove", "actor-remove"), actor);
        assertTrue(effects.calls.indexOf("hide 150") < effects.calls.indexOf("spot"));

        Placement spawned = effects.actorSpawns.get(0);
        assertEquals(12.5, spawned.point().x(), 1e-9);
        assertEquals(64.0, spawned.point().y(), 1e-9);
        assertEquals(10.0, spawned.point().z(), 1e-9);
        assertEquals(90f, spawned.yaw());

        assertEquals(1, effects.teleports.size());
        Placement turned = effects.teleports.get(0);
        assertEquals(origin.point(), turned.point());
        assertEquals(-90f, turned.yaw(), 1e-4);
        assertEquals(-Math.toDegrees(Math.atan2(2.6 - 1.62, 2.5)), turned.pitch(), 1e-4);

        assertEquals(8, effects.actorMoves.size());
        Placement last = effects.actorMoves.get(7);
        assertEquals(11.3, last.point().x(), 1e-9);
        assertEquals(10.0, last.point().z(), 1e-9);
        assertEquals(90f, last.yaw(), 1e-4);

        int emerge = effects.soundsHeard.indexOf("ENTITY_WARDEN_EMERGE");
        assertEquals(spawned.point(), effects.soundPlaces.get(emerge));
        int bite = effects.soundsHeard.indexOf("ENTITY_WARDEN_ATTACK_IMPACT");
        assertEquals(origin.point(), effects.soundPlaces.get(bite));
        int dig = effects.soundsHeard.indexOf("ENTITY_WARDEN_DIG");
        assertEquals(11.3, effects.soundPlaces.get(dig).x(), 1e-9);
        assertTrue(effects.calls.indexOf("actor-gesture ATTACK") < effects.calls.indexOf("hide 150"));
    }

    @Test
    void punishedPlayerWhoStaysOnlineIsNotReadAgainAndUnfrozenAtTheEnd() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, warden(), "warn Steve");
        while (effects.commands.isEmpty()) {
            director.tick(player);
        }
        effects.online.put(player, new Placement(new Point(500, 64, 500), 0f, 0f));
        while (director.tick(player)) {
            assertEquals(List.of("warn Steve"), effects.commands);
        }
        assertEquals(List.of("warn Steve"), effects.commands);
        assertTrue(effects.calls.contains("unfreeze"));
        assertTrue(effects.particleBursts.stream().flatMap(List::stream)
                .noneMatch(emission -> emission.at().x() > 100));
    }

    @Test
    void punishmentWithoutAPlaceEndsTheAnimationAtOnce() {
        AnimationScript script = new AnimationScript("x", 10, false, false, false, 5, -1, true, Optional.empty(),
                Optional.empty(), Optional.empty(), 3, List.of());
        RecordingEffects placeless = new RecordingEffects() {
            @Override
            public Optional<WorldSpot> spot(UUID who) {
                return Optional.empty();
            }
        };
        placeless.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(placeless, () -> 0.5, log);
        director.start(player, script, "ban x");
        assertTrue(director.tick(player));
        assertTrue(director.tick(player));
        assertFalse(director.tick(player));
        assertEquals(List.of("ban x"), placeless.commands);
        assertFalse(director.animating(player));
    }

    @Test
    void quittingMidWardenRemovesItAndRunsTheCommand() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, warden(), "ban a");
        for (int i = 0; i < 40; i++) {
            director.tick(player);
        }
        effects.online.remove(player);
        director.quit(player);
        assertEquals(List.of("ban a"), effects.commands);
        assertTrue(effects.calls.contains("actor-remove"));
    }

    @Test
    void abandoningRemovesTheWardenWithoutTheCommand() {
        effects.online.put(player, origin);
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, warden(), "ban a");
        director.tick(player);
        assertEquals(1, director.abandonAll());
        assertTrue(effects.calls.contains("actor-remove"));
        assertTrue(effects.commands.isEmpty());
    }

    @Test
    void olderServersPlayTheFallbackAnimation() {
        wardens = false;
        AnimationLibrary library = library();
        assertEquals(List.of("classic_ban", "dramatic_ban", "rainbow_ban", "warden_ban"), library.names());
        assertEquals("classic_ban", library.find("warden_ban").orElseThrow().name());
        assertEquals(List.of("[Animation] warden_ban cannot be shown on this server, classic_ban plays instead"),
                log.infos);
        assertEquals(List.of(), log.warnings);
    }

    @Test
    void missingFallbackMeansTheAnimationIsUnknown() {
        wardens = false;
        AnimationLibrary library = new AnimationLibrary(reader(), log);
        library.load(List.of(new AnimationFile("lonely.yml", () -> Map.of("actor", Map.of("type", "WARDEN"),
                "fallbackAnimation", "nothing_here", "stages", List.of()))));
        assertTrue(library.find("lonely").isEmpty());
    }

    @Test
    void unknownActorTypeFallsBackWithWarning() {
        AnimationLibrary library = new AnimationLibrary(reader(), log);
        library.load(List.of(file("classic_ban.yml"), new AnimationFile("ghast.yml", () -> Map.of(
                "actor", Map.of("type", "GHAST"), "stages", List.of()))));
        assertEquals("classic_ban", library.find("ghast").orElseThrow().name());
        assertEquals(List.of("[Animation] ghast.yml has an unknown actor type 'GHAST'"), log.warnings);
    }

    @Test
    void actorCuesAreReadWithDefaultsAndBadOnesSkipped() {
        AnimationScript script = reader().read("custom.yml", Map.of("actor", Map.of("type", "warden"),
                "stages", List.of(Map.of("actor", List.of(
                        Map.of("action", "spawn", "pose", "emerging"),
                        Map.of("action", "STEP", "atTick", 4, "distance", 0.5, "ticks", 0),
                        Map.of("action", "POSE", "pose", "DANCING"),
                        Map.of("action", "FLY"))))));
        assertEquals(Optional.of(new ActorSpec(ActorKind.WARDEN, ScriptReader.DEFAULT_DISTANCE_BEHIND)),
                script.actor());
        assertEquals(List.of(new ActorCue(0, ActorAction.SPAWN, ActorPose.EMERGING, 1.0, 1),
                new ActorCue(4, ActorAction.STEP, ActorPose.STANDING, 0.5, 1),
                new ActorCue(0, ActorAction.POSE, ActorPose.STANDING, 1.0, 1)),
                script.stages().get(0).actorCues());
        assertEquals(List.of("[Animation] Unknown actor pose 'DANCING', using STANDING",
                "[Animation] Unknown actor action 'FLY', skipping it"), log.warnings);
    }

    @Test
    void actorCuesBeforeSpawnDoNothing() {
        ScriptStage stage = new ScriptStage("s", 1, 6, List.of(), List.of(), List.of(), List.of(), Optional.empty(),
                List.of(new ActorCue(0, ActorAction.POSE, ActorPose.ROARING, 1, 1),
                        new ActorCue(1, ActorAction.ATTACK, ActorPose.STANDING, 1, 1),
                        new ActorCue(2, ActorAction.TURN_VICTIM, ActorPose.STANDING, 1, 1),
                        new ActorCue(3, ActorAction.REMOVE, ActorPose.STANDING, 1, 1),
                        new ActorCue(4, ActorAction.SPAWN, ActorPose.STANDING, 1, 1),
                        new ActorCue(5, ActorAction.SPAWN, ActorPose.EMERGING, 1, 1)));
        AnimationScript script = new AnimationScript("x", 8, false, false, false, 5, -1, true, Optional.empty(),
                Optional.of(new ActorSpec(ActorKind.WARDEN, 3)), Optional.empty(), 0, List.of(stage));
        effects.online.put(player, new Placement(new Point(0, 70, 0), 0f, 0f));
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, script, "cmd");
        while (director.tick(player)) {
            effects.calls.add("tick");
        }
        assertEquals(List.of("actor-spawn WARDEN STANDING", "actor-remove"), effects.calls.stream()
                .filter(call -> call.startsWith("actor")).toList());
        assertTrue(effects.teleports.isEmpty());
        assertEquals(new Point(0, 70, -3), effects.actorSpawns.get(0).point());
    }

    @Test
    void actorAnchoredParticlesFollowTheActor() {
        ParticleLook look = ParticleLook.of("FLAME", false, 0, 0, 0, 1);
        ParticleEffect atActor = new ParticleEffect(look, ShapePattern.POINT, 1, 1, 0, 0, 0, 0, 0, new Point(0, 0, 0),
                Anchor.ACTOR);
        ScriptStage stage = new ScriptStage("s", 1, 3, List.of(atActor), List.of(), List.of(), List.of(),
                Optional.empty(), List.of(new ActorCue(1, ActorAction.SPAWN, ActorPose.EMERGING, 1, 1)));
        AnimationScript script = new AnimationScript("x", 3, false, false, false, 5, -1, true, Optional.empty(),
                Optional.of(new ActorSpec(ActorKind.WARDEN, 2)), Optional.empty(), 0, List.of(stage));
        effects.online.put(player, new Placement(new Point(0, 70, 0), 180f, 0f));
        AnimationDirector director = new AnimationDirector(effects, () -> 0.5, log);
        director.start(player, script, "cmd");
        while (director.tick(player)) {
            assertTrue(effects.commands.isEmpty());
        }
        assertEquals(new Point(0, 70, 0), effects.particleBursts.get(0).get(0).at());
        Point behind = effects.particleBursts.get(1).get(0).at();
        assertEquals(0.0, behind.x(), 1e-9);
        assertEquals(2.0, behind.z(), 1e-9);
    }
}
