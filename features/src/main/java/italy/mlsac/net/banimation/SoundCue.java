// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record SoundCue(String sound, int soundTick, float volume, float pitch, boolean loop, int repeatEveryTicks,
                       Anchor anchor) {

    public SoundCue(String sound, int soundTick, float volume, float pitch, boolean loop, int repeatEveryTicks) {
        this(sound, soundTick, volume, pitch, loop, repeatEveryTicks, Anchor.VICTIM);
    }

    public boolean playsAt(int stageTick) {
        return loop ? stageTick % repeatEveryTicks == 0 : stageTick == soundTick;
    }
}
