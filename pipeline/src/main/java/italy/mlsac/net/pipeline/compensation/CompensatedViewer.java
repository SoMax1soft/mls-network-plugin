// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CompensatedViewer {
    private final ClientQuirks profile;
    private final PingAcknowledgements acknowledgements;
    private final Map<Integer, AcknowledgedBody> entities = new HashMap<>();

    public CompensatedViewer(ClientQuirks profile, PingAcknowledgements acknowledgements) {
        this.profile = profile;
        this.acknowledgements = acknowledgements;
    }

    public PingAcknowledgements acknowledgements() {
        return acknowledgements;
    }

    public void spawn(int entityNumber, Vec3 position) {
        entities.put(entityNumber, new AcknowledgedBody(profile, position));
    }

    public void destroy(int entityNumber) {
        entities.remove(entityNumber);
    }

    public void reset() {
        entities.clear();
    }

    public void move(EntityMove move) {
        AcknowledgedBody entity = entities.get(move.entityNumber());
        if (entity == null) {
            return;
        }
        if (entity.pingMarker() == acknowledgements.probesFlushed() && acknowledgements.backlogSmall()) {
            acknowledgements.sendProbe();
        }
        entity.pingMarker(acknowledgements.probesFlushed());
        int acknowledgedAt = acknowledgements.probesFlushed();
        acknowledgements.afterAcknowledged(acknowledgedAt, () -> entity.acknowledge(move));
        acknowledgements.afterAcknowledged(acknowledgedAt + 1, entity::settle);
    }

    public void clientTick() {
        for (AcknowledgedBody entity : entities.values()) {
            entity.clientTick();
        }
    }

    public Optional<AcknowledgedBody> entity(int entityNumber) {
        return Optional.ofNullable(entities.get(entityNumber));
    }

    public AcknowledgedBody entityOrNull(int entityNumber) {
        return entities.get(entityNumber);
    }
}
