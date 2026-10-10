// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;
import java.util.UUID;

public sealed interface AnimationCue permits AnimationCue.SoundAt, AnimationCue.TitleShown, AnimationCue.LightningAt,
        AnimationCue.ParticlesAt, AnimationCue.ItemDropped, AnimationCue.MovedTo {

    void apply(AnimationEffects effects, UUID player);

    record SoundAt(Point at, SoundCue sound) implements AnimationCue {
        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.sound(player, at, sound.sound(), sound.volume(), sound.pitch());
        }
    }

    record TitleShown(TitleCue title) implements AnimationCue {
        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.title(player, title);
        }
    }

    record LightningAt(Point at) implements AnimationCue {
        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.lightning(player, at);
        }
    }

    record ParticlesAt(ParticleLook look, List<Emission> emissions, Point spread, double speed) implements AnimationCue {

        public ParticlesAt {
            emissions = List.copyOf(emissions);
        }

        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.particles(player, look, emissions, spread, speed);
        }
    }

    record ItemDropped(int index, Point at) implements AnimationCue {
        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.dropItem(player, index, at);
        }
    }

    record MovedTo(Placement target) implements AnimationCue {
        @Override
        public void apply(AnimationEffects effects, UUID player) {
            effects.teleport(player, target);
        }
    }
}
