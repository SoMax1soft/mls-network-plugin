// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.nametag.MetaEntry;
import italy.mlsac.net.nametag.MetaValue;
import italy.mlsac.net.nametag.NametagPacket;
import italy.mlsac.net.nametag.SpawnPacketKind;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NametagWireTest {

    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    private final NametagWire wire = new NametagWire(packets.sender(), new LegacyColorizer(new HexColorNormalizer()));
    private final UUID viewer = UUID.nameUUIDFromBytes("Mod".getBytes());
    private final UUID tag = new UUID(0x2E9B61F07AC4D153L, 2147483646L);

    @Test
    void everyPacketKindIsWrittenInOrderAndFlushedOnce() {
        wire.send(viewer, List.of(
                new NametagPacket.SpawnTextDisplay(2147483646, tag, 1, 65.8, 2),
                new NametagPacket.SpawnArmorStand(2147483646, tag, 1, 66.3, 2, SpawnPacketKind.SPAWN_ENTITY),
                new NametagPacket.Metadata(2147483646, List.of(
                        new MetaEntry(0, new MetaValue.ByteValue((byte) 0x20)),
                        new MetaEntry(24, new MetaValue.IntValue(7)),
                        new MetaEntry(17, new MetaValue.FloatValue(0.5f)),
                        new MetaEntry(3, new MetaValue.BooleanValue(true)),
                        new MetaEntry(11, new MetaValue.Vector3fValue(0f, 0.72f, 0f)))),
                new NametagPacket.Passengers(77, List.of(5, 2147483646)),
                new NametagPacket.RelativeMove(2147483646, 0.25, 0, -0.5),
                new NametagPacket.Teleport(2147483646, 3, 66.3, 4),
                new NametagPacket.Destroy(List.of(2147483646, 2147483645))));
        List<String> sent = packets.sent();
        assertEquals(8, sent.size());
        assertTrue(sent.get(0).startsWith("spawn " + viewer + " VirtualEntity[entityNumber=2147483646"));
        assertTrue(sent.get(0).contains("type=TEXT_DISPLAY"));
        assertTrue(sent.get(1).contains("type=ARMOR_STAND"));
        assertEquals("metadata " + viewer + " 2147483646 [ByteValue[index=0, value=32], IntValue[index=24, value=7], "
                + "FloatValue[index=17, value=0.5], BooleanValue[index=3, value=true], "
                + "VectorValue[index=11, x=0.0, y=0.72, z=0.0]]", sent.get(2));
        assertEquals("passengers " + viewer + " 77 [5, 2147483646]", sent.get(3));
        assertEquals("move " + viewer + " 2147483646 0.25 0.0 -0.5", sent.get(4));
        assertEquals("teleport " + viewer + " 2147483646 3.0 66.3 4.0", sent.get(5));
        assertEquals("destroy " + viewer + " [2147483646, 2147483645]", sent.get(6));
        assertEquals("flush " + viewer, sent.get(7));
    }

    @Test
    void textIsTurnedIntoSectionCodesWithHexForTheComponentEncoder() {
        wire.send(viewer, List.of(new NametagPacket.Metadata(9, List.of(
                new MetaEntry(23, new MetaValue.TextValue("&f&lAVG: &#FF0000 76%")),
                new MetaEntry(2, new MetaValue.OptionalTextValue("&7none"))))));
        assertEquals("metadata " + viewer + " 9 [TextValue[index=23, coloredText=§f§lAVG: §x§F§F§0§0§0§0 76%], "
                + "OptionalTextValue[index=2, coloredText=Optional[§7none]]]", packets.sent().get(0));
    }

    @Test
    void anEmptyBatchSendsNothing() {
        wire.send(viewer, List.of());
        assertTrue(packets.sent().isEmpty());
    }
}
