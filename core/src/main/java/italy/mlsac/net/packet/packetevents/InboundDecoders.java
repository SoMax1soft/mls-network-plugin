// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.ClientTickEndPacket;
import italy.mlsac.net.packet.HeldSlotPacket;
import italy.mlsac.net.packet.InboundPacket;
import italy.mlsac.net.packet.MovementPacket;
import italy.mlsac.net.packet.PlayerActionKind;
import italy.mlsac.net.packet.PlayerActionPacket;
import italy.mlsac.net.packet.PongPacket;
import italy.mlsac.net.packet.SwingPacket;
import italy.mlsac.net.packet.TeleportConfirmPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InboundDecoders {

    @FunctionalInterface
    interface Decoder {
        Optional<InboundPacket> decode(PacketReceiveEvent event, UUID player, long receivedNanos);
    }

    record Entry(Class<? extends InboundPacket> produces, Decoder decoder) {
    }

    private final Map<PacketTypeCommon, List<Entry>> entries = new HashMap<>();

    InboundDecoders() {
        Entry movement = new Entry(MovementPacket.class, InboundDecoders::movement);
        add(PacketType.Play.Client.PLAYER_FLYING, movement);
        add(PacketType.Play.Client.PLAYER_POSITION, movement);
        add(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, movement);
        add(PacketType.Play.Client.PLAYER_ROTATION, movement);
        add(PacketType.Play.Client.CLIENT_TICK_END, new Entry(ClientTickEndPacket.class,
                (event, player, receivedNanos) -> Optional.of(new ClientTickEndPacket(player, receivedNanos))));
        add(PacketType.Play.Client.TELEPORT_CONFIRM, new Entry(TeleportConfirmPacket.class,
                (event, player, receivedNanos) -> Optional.of(new TeleportConfirmPacket(player,
                        new WrapperPlayClientTeleportConfirm(event).getTeleportId()))));
        add(PacketType.Play.Client.ANIMATION, new Entry(SwingPacket.class,
                (event, player, receivedNanos) -> Optional.of(new SwingPacket(player, receivedNanos,
                        new WrapperPlayClientAnimation(event).getHand() == InteractionHand.MAIN_HAND))));
        add(PacketType.Play.Client.HELD_ITEM_CHANGE, new Entry(HeldSlotPacket.class,
                (event, player, receivedNanos) -> Optional.of(new HeldSlotPacket(player,
                        new WrapperPlayClientHeldItemChange(event).getSlot()))));
        add(PacketType.Play.Client.PUNCH, new Entry(SwingPacket.class,
                (event, player, receivedNanos) -> Optional.of(new SwingPacket(player, receivedNanos, true))));
        add(PacketType.Play.Client.INTERACT_ENTITY, new Entry(AttackPacket.class, InboundDecoders::attack));
        add(PacketType.Play.Client.ATTACK, new Entry(AttackPacket.class,
                (event, player, receivedNanos) -> Optional.of(new AttackPacket(player, receivedNanos,
                        new WrapperPlayClientAttack(event).getEntityId(), event.isCancelled()))));
        add(PacketType.Play.Client.PONG, new Entry(PongPacket.class,
                (event, player, receivedNanos) -> Optional.of(new PongPacket(player, new WrapperPlayClientPong(event).getId()))));
        add(PacketType.Play.Client.WINDOW_CONFIRMATION, new Entry(PongPacket.class,
                (event, player, receivedNanos) -> Optional.of(new PongPacket(player,
                        new WrapperPlayClientWindowConfirmation(event).getActionId()))));
        addPlayerActions();
    }

    List<Entry> entriesFor(PacketTypeCommon type) {
        return entries.getOrDefault(type, List.of());
    }

    private void addPlayerActions() {
        action(PacketType.Play.Client.CLICK_WINDOW, PlayerActionKind.WINDOW_CLICK);
        action(PacketType.Play.Client.CLOSE_WINDOW, PlayerActionKind.WINDOW_CLOSE);
        action(PacketType.Play.Client.WINDOW_CONFIRMATION, PlayerActionKind.WINDOW_CONFIRMATION);
        action(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION, PlayerActionKind.CREATIVE_INVENTORY_ACTION);
        action(PacketType.Play.Client.PICK_ITEM, PlayerActionKind.PICK_ITEM);
        action(PacketType.Play.Client.PICK_ITEM_FROM_BLOCK, PlayerActionKind.PICK_ITEM);
        action(PacketType.Play.Client.PICK_ITEM_FROM_ENTITY, PlayerActionKind.PICK_ITEM);
        action(PacketType.Play.Client.HELD_ITEM_CHANGE, PlayerActionKind.HELD_ITEM_CHANGE);
        action(PacketType.Play.Client.PLAYER_DIGGING, PlayerActionKind.DIGGING);
        action(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT, PlayerActionKind.BLOCK_PLACEMENT);
        action(PacketType.Play.Client.USE_ITEM, PlayerActionKind.USE_ITEM);
        action(PacketType.Play.Client.INTERACT_ENTITY, PlayerActionKind.INTERACT_ENTITY);
        action(PacketType.Play.Client.ATTACK, PlayerActionKind.INTERACT_ENTITY);
        action(PacketType.Play.Client.ENTITY_ACTION, PlayerActionKind.ENTITY_ACTION);
        action(PacketType.Play.Client.PLAYER_ABILITIES, PlayerActionKind.ABILITIES);
        action(PacketType.Play.Client.CLIENT_STATUS, PlayerActionKind.CLIENT_STATUS);
    }

    private void action(PacketTypeCommon type, PlayerActionKind kind) {
        add(type, new Entry(PlayerActionPacket.class,
                (event, player, receivedNanos) -> Optional.of(new PlayerActionPacket(player, kind))));
    }

    private void add(PacketTypeCommon type, Entry entry) {
        entries.computeIfAbsent(type, key -> new ArrayList<>(2)).add(entry);
    }

    private static Optional<InboundPacket> movement(PacketReceiveEvent event, UUID player, long receivedNanos) {
        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        Location location = flying.getLocation();
        boolean rotation = flying.hasRotationChanged();
        boolean position = flying.hasPositionChanged();
        return Optional.of(new MovementPacket(player, receivedNanos,
                rotation ? location.getYaw() : 0f, rotation ? location.getPitch() : 0f, rotation,
                position ? location.getX() : 0.0, position ? location.getY() : 0.0, position ? location.getZ() : 0.0,
                position, flying.isOnGround(), event.isCancelled()));
    }

    private static Optional<InboundPacket> attack(PacketReceiveEvent event, UUID player, long receivedNanos) {
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return Optional.empty();
        }
        return Optional.of(new AttackPacket(player, receivedNanos, interact.getEntityId(), event.isCancelled()));
    }
}
