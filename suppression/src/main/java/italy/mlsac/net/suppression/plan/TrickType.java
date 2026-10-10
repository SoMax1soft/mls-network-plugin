// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public enum TrickType {
    SHUFFLE_INVENTORY(new TrickProfile("shuffle_inventory", "shuffle", "suppression-shuffle-inventory", 1.4, 0.45)),
    DROP_WEAPON(new TrickProfile("drop_weapon", "drop weapon", "suppression-drop-weapon", 1.9, 0.55)),
    LAUNCH(new TrickProfile("launch", "launch", "suppression-launch", 1.2, 0.6));

    private final TrickProfile profile;

    TrickType(TrickProfile profile) {
        this.profile = profile;
    }

    public TrickProfile profile() {
        return profile;
    }
}
