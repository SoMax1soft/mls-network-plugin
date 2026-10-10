// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;
import java.util.UUID;

final class Sighting {

    private final UUID suspect;
    private final int entityNumber;
    private double x;
    private double y;
    private double z;
    private SuspectPose pose = SuspectPose.STANDING;
    private List<Integer> passengers = List.of();
    private ShownTag shown;

    Sighting(UUID suspect, int entityNumber, double x, double y, double z) {
        this.suspect = suspect;
        this.entityNumber = entityNumber;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    UUID suspect() {
        return suspect;
    }

    SuspectView view() {
        return new SuspectView(entityNumber, x, y, z, pose, passengers);
    }

    void shiftBy(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
    }

    void moveTo(double newX, double newY, double newZ) {
        x = newX;
        y = newY;
        z = newZ;
    }

    boolean standsAt(double atX, double atY, double atZ) {
        return x == atX && y == atY && z == atZ;
    }

    SuspectPose pose() {
        return pose;
    }

    void pose(SuspectPose newPose) {
        pose = newPose;
    }

    void passengers(List<Integer> serverPassengers) {
        passengers = List.copyOf(serverPassengers);
    }

    boolean shown() {
        return shown != null;
    }

    ShownTag shownTag() {
        return shown;
    }

    void markShown(ShownTag tag) {
        shown = tag;
    }

    void markHidden() {
        shown = null;
    }
}
