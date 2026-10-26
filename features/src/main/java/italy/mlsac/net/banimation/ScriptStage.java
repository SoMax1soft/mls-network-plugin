// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;
import java.util.Optional;

public record ScriptStage(String name, int fromTick, int untilTick, List<ParticleEffect> particles,
                          List<SoundCue> sounds, List<TitleCue> titles, List<Integer> boltTicks,
                          Optional<ParticleTrail> trail, List<ActorCue> actorCues) {

    public ScriptStage {
        particles = List.copyOf(particles);
        sounds = List.copyOf(sounds);
        titles = List.copyOf(titles);
        boltTicks = List.copyOf(boltTicks);
        actorCues = List.copyOf(actorCues);
    }

    public ScriptStage(String name, int fromTick, int untilTick, List<ParticleEffect> particles,
                       List<SoundCue> sounds, List<TitleCue> titles, List<Integer> boltTicks,
                       Optional<ParticleTrail> trail) {
        this(name, fromTick, untilTick, particles, sounds, titles, boltTicks, trail, List.of());
    }

    public boolean activeAt(int tick) {
        return fromTick <= tick && tick <= untilTick;
    }

    public double progressAt(int tick) {
        int duration = untilTick - fromTick;
        return duration <= 0 ? 1.0 : (double) (tick - fromTick) / duration;
    }
}
