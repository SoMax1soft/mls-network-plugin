// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.damage;

public interface DamageEventModel {

    double finalDamage();

    boolean scaleComponents(double factor);

    double baseDamage();

    void baseDamage(double value);

    void cancel();
}
