// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import java.util.UUID;

final class PacketEventsChannels implements ChannelAccess {

    private final PacketEventsAPI<?> api;

    PacketEventsChannels(PacketEventsAPI<?> api) {
        this.api = api;
    }

    @Override
    public Object channelOf(UUID player) {
        return api.getProtocolManager().getChannel(player);
    }

    @Override
    public boolean open(Object channel) {
        return ChannelHelper.isOpen(channel);
    }

    @Override
    public boolean injected(Object channel) {
        String encoder = PacketEvents.ENCODER_NAME;
        String decoder = PacketEvents.DECODER_NAME;
        return encoder != null && decoder != null && ChannelHelper.getPipelineHandler(channel, encoder) != null
                && ChannelHelper.getPipelineHandler(channel, decoder) != null;
    }

    @Override
    public void execute(Object channel, Runnable task) {
        ChannelHelper.runInEventLoop(channel, task);
    }
}
