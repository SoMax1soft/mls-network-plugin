// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public enum SpawnPacketKind {
    SPAWN_LIVING_ENTITY,
    SPAWN_ENTITY;

    public static SpawnPacketKind forServer(GameRelease server) {
        return server.atLeast(GameRelease.GENERIC_LIVING_SPAWN) ? SPAWN_ENTITY : SPAWN_LIVING_ENTITY;
    }
}
