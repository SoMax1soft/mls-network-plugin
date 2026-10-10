// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

public record DamageDealtAction(int stage, int percent, int seconds) implements SuppressionAction, TimedAction {

    public static final String TYPE = "damage_dealt";

    @Override
    public String kindKey() {
        return TYPE;
    }
}
