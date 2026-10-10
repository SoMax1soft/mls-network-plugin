// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public record ParticleEffect(ParticleLook look, ShapePattern shape, int emitEveryTicks, int count, double spreadFrom,
                             double spreadTo, double liftAtStart, double liftAtEnd, double speed,
                             Point spread) {

    public double radiusAt(double progress) {
        return spreadFrom + (spreadTo - spreadFrom) * progress;
    }

    public double heightAt(double progress) {
        return liftAtStart + (liftAtEnd - liftAtStart) * progress;
    }
}
