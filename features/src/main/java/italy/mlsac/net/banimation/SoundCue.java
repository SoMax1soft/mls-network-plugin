// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record SoundCue(String sound, int soundTick, float volume, float pitch, boolean loop, int repeatEveryTicks) {

    public boolean playsAt(int stageTick) {
        return loop ? stageTick % repeatEveryTicks == 0 : stageTick == soundTick;
    }
}
