// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import italy.mlsac.net.packet.MetadataEntry;
import java.util.ArrayList;
import java.util.List;

final class MetadataEncoder {

    List<EntityData<?>> encode(List<MetadataEntry> entries) {
        List<EntityData<?>> encoded = new ArrayList<>(entries.size());
        for (MetadataEntry entry : entries) {
            encoded.add(encode(entry));
        }
        return encoded;
    }

    private static EntityData<?> encode(MetadataEntry entry) {
        if (entry instanceof MetadataEntry.ByteValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.BYTE, value.value());
        }
        if (entry instanceof MetadataEntry.IntValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.INT, value.value());
        }
        if (entry instanceof MetadataEntry.FloatValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.FLOAT, value.value());
        }
        if (entry instanceof MetadataEntry.BooleanValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.BOOLEAN, value.value());
        }
        if (entry instanceof MetadataEntry.VectorValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.VECTOR3F, new Vector3f(value.x(), value.y(), value.z()));
        }
        if (entry instanceof MetadataEntry.TextValue value) {
            return new EntityData<>(value.index(), EntityDataTypes.ADV_COMPONENT,
                    AdventureSerializer.serializer().fromLegacy(value.coloredText()));
        }
        MetadataEntry.OptionalTextValue value = (MetadataEntry.OptionalTextValue) entry;
        return new EntityData<>(value.index(), EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                value.coloredText().map(text -> AdventureSerializer.serializer().fromLegacy(text)));
    }
}
