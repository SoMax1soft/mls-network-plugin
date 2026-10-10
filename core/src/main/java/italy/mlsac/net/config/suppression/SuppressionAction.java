// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

public sealed interface SuppressionAction
        permits CommandAction, DamageDealtAction, DamageTakenAction, LagAction, TrickAction {

    int stage();

    String kindKey();
}
