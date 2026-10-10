// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.UUID;

public interface StaffMember {

    UUID id();

    String name();

    boolean online();

    boolean staff();

    boolean operator();

    void send(String coloredText);

    void playSound(String sound, float volume, float pitch);

    default boolean staffOrOp() {
        return operator() || staff();
    }
}
