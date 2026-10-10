// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.damage.DamageEventModel;

public final class FakeDamageEvent implements DamageEventModel {

    private final double armourShare;
    private final boolean componentsSupported;
    private double base;
    private double armour;
    private boolean cancelled;
    private int writes;

    private FakeDamageEvent(double base, double armourShare, boolean componentsSupported) {
        this.base = base;
        this.armourShare = armourShare;
        this.armour = -base * armourShare;
        this.componentsSupported = componentsSupported;
    }

    public static FakeDamageEvent withFinal(double finalDamage) {
        return new FakeDamageEvent(finalDamage / 0.6, 0.4, true);
    }

    public static FakeDamageEvent withoutComponents(double finalDamage) {
        return new FakeDamageEvent(finalDamage / 0.6, 0.4, false);
    }

    public boolean cancelled() {
        return cancelled;
    }

    public int writes() {
        return writes;
    }

    @Override
    public double finalDamage() {
        return Math.max(0.0, base + armour);
    }

    @Override
    public boolean scaleComponents(double factor) {
        if (!componentsSupported) {
            throw new UnsupportedOperationException("modifiers removed");
        }
        writes++;
        base *= factor;
        armour *= factor;
        return true;
    }

    @Override
    public double baseDamage() {
        return base;
    }

    @Override
    public void baseDamage(double value) {
        writes++;
        base = value;
        armour = -base * armourShare;
    }

    @Override
    public void cancel() {
        writes++;
        cancelled = true;
    }
}
