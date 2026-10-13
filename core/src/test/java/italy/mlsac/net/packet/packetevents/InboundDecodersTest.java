// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.InboundPacket;
import italy.mlsac.net.packet.PlayerActionPacket;
import italy.mlsac.net.packet.SwingPacket;
import org.junit.jupiter.api.Test;

class InboundDecodersTest {

    private final InboundDecoders decoders = new InboundDecoders();

    private boolean produces(PacketTypeCommon type, Class<? extends InboundPacket> packet) {
        return decoders.entriesFor(type).stream().anyMatch(entry -> entry.produces() == packet);
    }

    @Test
    void attacksAreReadFromTheOldAndTheNewAttackPacket() {
        assertTrue(produces(PacketType.Play.Client.INTERACT_ENTITY, AttackPacket.class));
        assertTrue(produces(PacketType.Play.Client.ATTACK, AttackPacket.class));
        assertTrue(produces(PacketType.Play.Client.ATTACK, PlayerActionPacket.class));
    }

    @Test
    void swingsAreReadFromTheOldAndTheNewSwingPacket() {
        assertTrue(produces(PacketType.Play.Client.ANIMATION, SwingPacket.class));
        assertTrue(produces(PacketType.Play.Client.PUNCH, SwingPacket.class));
    }
}
