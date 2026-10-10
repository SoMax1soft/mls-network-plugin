// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.Optional;

final class OwnTagSeat {

    private int entityNumber;
    private ClientStage stage = ClientStage.IN_WORLD;
    private SeedSighting place;

    OwnTagSeat(int entityNumber) {
        this.entityNumber = entityNumber;
    }

    int entityNumber() {
        return entityNumber;
    }

    void renumber(int newNumber) {
        entityNumber = newNumber;
        place = null;
    }

    void enter(ClientStage newStage) {
        stage = newStage;
    }

    boolean acceptsEntities() {
        return stage.acceptsEntities();
    }

    void place(double x, double y, double z) {
        place = new SeedSighting(entityNumber, x, y, z);
    }

    Optional<SeedSighting> place() {
        return Optional.ofNullable(place);
    }
}
