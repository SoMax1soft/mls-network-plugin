// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public enum DestroyStyle {
    APPEND_TO_SERVER_PACKET {
        @Override
        DestroyReaction react(List<Integer> tagIds) {
            return new DestroyReaction(tagIds, List.of());
        }
    },
    SEPARATE_PACKET_PER_TAG {
        @Override
        DestroyReaction react(List<Integer> tagIds) {
            return new DestroyReaction(List.of(),
                    tagIds.stream().map(id -> (NametagPacket) new NametagPacket.Destroy(List.of(id))).toList());
        }
    };

    public static DestroyStyle forServer(GameRelease server) {
        return server.equals(GameRelease.SINGLE_ID_DESTROY) ? SEPARATE_PACKET_PER_TAG : APPEND_TO_SERVER_PACKET;
    }

    abstract DestroyReaction react(List<Integer> tagIds);
}
