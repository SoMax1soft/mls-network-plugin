// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public enum ClientStage {
    IN_WORLD,
    CONFIGURING;

    public boolean acceptsEntities() {
        return this == IN_WORLD;
    }
}
