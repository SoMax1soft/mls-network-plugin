// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record FreezeSpec(int slownessAmplifier, int jumpAmplifier, int slowTicks, int levitationAmplifier,
                         int levitationTicks) {

    static final int SLOWNESS = 255;
    static final int JUMP = 128;
    static final int EXTRA_SLOW_TICKS = 40;

    public static FreezeSpec of(AnimationScript script) {
        return new FreezeSpec(SLOWNESS, JUMP, script.lengthTicks() + EXTRA_SLOW_TICKS, script.effectiveLevitation(),
                script.lengthTicks());
    }
}
