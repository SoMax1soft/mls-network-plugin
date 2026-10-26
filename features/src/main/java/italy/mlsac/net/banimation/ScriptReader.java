// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.logbook.Logbook;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ScriptReader {

    static final String DEFAULT_SOUND = "ENTITY_EXPERIENCE_ORB_PICKUP";
    static final double DEFAULT_DISTANCE_BEHIND = 2.5;

    private final ParticleNames particles;
    private final EffectRegistry registry;
    private final StageExtras extras;
    private final Logbook log;

    public ScriptReader(ParticleNames particles, EffectRegistry registry, StageExtras extras, Logbook log) {
        this.particles = particles;
        this.registry = registry;
        this.extras = extras;
        this.log = log;
    }

    public AnimationScript read(String fileName, Map<?, ?> document) {
        ScriptNode root = new ScriptNode(document);
        String name = fileName.toLowerCase(Locale.ROOT).replaceFirst("\\.yml$", "");
        if (!root.has("stages")) {
            log.warn("[Animation] " + fileName + " has no stages");
        }
        Optional<ScriptNode> actorNode = root.child("actor");
        Optional<ActorSpec> actor = actorNode.flatMap(node -> actor(fileName, node));
        if (actorNode.isPresent() && actor.filter(spec -> registry.hasActor(spec.kind())).isEmpty()) {
            String instead = root.text("fallbackAnimation", AnimationLibrary.DEFAULT_ANIMATION)
                    .toLowerCase(Locale.ROOT);
            log.info("[Animation] " + name + " cannot be shown on this server, " + instead + " plays instead");
            return new AnimationScript(name, root.integer("totalTicks", 80), false, false, false, 1, -1, false,
                    Optional.empty(), Optional.empty(), Optional.of(instead), 0, List.of());
        }
        boolean extended = extras.reads() || actor.isPresent();
        return new AnimationScript(name,
                root.integer("totalTicks", 80),
                root.flag("freezePlayer", true),
                root.flag("strikeLightningAtEnd", true),
                root.flag("stripPlayer", false),
                root.integer("itemDropIntervalTicks", 5),
                root.integer("customLevitationAmplifier", -1),
                root.flag("levitatePlayer", true),
                root.child("movement").map(ScriptReader::movement),
                actor,
                Optional.empty(),
                Math.max(0, root.integer("punishAtTick", 0)),
                root.children("stages").stream().map(node -> stage(node, extended)).toList());
    }

    private Optional<ActorSpec> actor(String fileName, ScriptNode node) {
        String type = node.text("type", "");
        Optional<ActorKind> kind = ActorKind.named(type);
        if (kind.isEmpty()) {
            log.warn("[Animation] " + fileName + " has an unknown actor type '" + type + "'");
        }
        return kind.map(found -> new ActorSpec(found, node.decimal("distanceBehind", DEFAULT_DISTANCE_BEHIND)));
    }

    private static MovementPath movement(ScriptNode node) {
        return new MovementPath(node.integer("startTick", 0), node.integer("endTick", 0),
                node.decimal("offsetX", 0.0), node.decimal("offsetY", 0.0), node.decimal("offsetZ", 0.0),
                node.flag("smooth", true));
    }

    private ScriptStage stage(ScriptNode node, boolean extended) {
        List<TitleCue> titles = extended ? node.children("titles").stream().map(ScriptReader::title).toList()
                : List.of();
        Optional<ParticleTrail> trail = extended ? node.child("particleTrail").map(this::trail)
                : Optional.empty();
        return new ScriptStage(node.text("name", "Unnamed Stage"), node.integer("startTick", 0),
                node.integer("endTick", 0),
                node.children("particles").stream().map(this::particle).toList(),
                node.children("sounds").stream().map(this::sound).toList(),
                titles, node.integers("lightningTicks"), trail,
                node.children("actor").stream().flatMap(cue -> actorCue(cue).stream()).toList());
    }

    private Optional<ActorCue> actorCue(ScriptNode node) {
        String requested = node.text("action", "");
        Optional<ActorAction> action = ActorAction.named(requested);
        if (action.isEmpty()) {
            log.warn("[Animation] Unknown actor action '" + requested + "', skipping it");
            return Optional.empty();
        }
        String poseName = node.text("pose", ActorPose.STANDING.name());
        ActorPose pose = ActorPose.named(poseName).orElseGet(() -> {
            log.warn("[Animation] Unknown actor pose '" + poseName + "', using " + ActorPose.STANDING);
            return ActorPose.STANDING;
        });
        return Optional.of(new ActorCue(node.integer("atTick", 0), action.get(), pose,
                node.decimal("distance", 1.0), positive(node.integer("ticks", 1))));
    }

    private ParticleEffect particle(ScriptNode node) {
        return new ParticleEffect(look(node), ShapePattern.named(node.text("shape", "POINT")),
                positive(node.integer("intervalTicks", 1)), node.integer("count", 1),
                node.decimal("radiusStart", 1.0), node.decimal("radiusEnd", 1.0),
                node.decimal("heightOffsetStart", 0.0), node.decimal("heightOffsetEnd", 0.0),
                node.decimal("speed", 0.0), spread(node, 0.0), Anchor.named(node.text("anchor", "VICTIM")));
    }

    private ParticleTrail trail(ScriptNode node) {
        return new ParticleTrail(look(node), positive(node.integer("intervalTicks", 2)), node.integer("count", 3),
                spread(node, 0.2));
    }

    private ParticleLook look(ScriptNode node) {
        return ParticleLook.of(particles.resolve(node.text("particleType", ParticleNames.FALLBACK)),
                node.flag("isColored", false), node.integer("red", 255), node.integer("green", 255),
                node.integer("blue", 255), node.decimal("particleSize", 1.0));
    }

    private SoundCue sound(ScriptNode node) {
        String requested = node.text("soundType", DEFAULT_SOUND);
        String sound = requested.trim().toUpperCase(Locale.ROOT);
        if (!registry.hasSound(sound)) {
            log.warn("[Animation] Unknown sound '" + requested + "', using " + DEFAULT_SOUND);
            sound = DEFAULT_SOUND;
        }
        return new SoundCue(sound, node.integer("playAtTick", 0), node.single("volume", 1.0f),
                node.single("pitch", 1.0f), node.flag("loop", false), positive(node.integer("loopIntervalTicks", 20)),
                Anchor.named(node.text("anchor", "VICTIM")));
    }

    private static TitleCue title(ScriptNode node) {
        return new TitleCue(node.integer("showAtTick", 0), node.text("title", ""), node.text("subtitle", ""),
                node.integer("fadeIn", 10), node.integer("stay", 40), node.integer("fadeOut", 10));
    }

    private static Point spread(ScriptNode node, double fallback) {
        return new Point(node.decimal("offsetX", fallback), node.decimal("offsetY", fallback),
                node.decimal("offsetZ", fallback));
    }

    private static int positive(int interval) {
        return Math.max(1, interval);
    }
}
