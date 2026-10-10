// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.server.SuspectBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FakeBody implements SuspectBody {

    public static final int STORAGE = 36;

    private final UUID id;
    private final Timeline timeline;
    private final String[] storage = new String[STORAGE];
    private final String[] armour = {"HELMET", "CHEST", "LEGS", "BOOTS"};
    private final String offHand = "SHIELD";
    private final String cursor = "CURSOR_ITEM";
    private final int selectedSlot = 3;
    private String mainHand = "DIAMOND_SWORD";
    private Vec3 look = new Vec3(0, 0, 1);
    private boolean dead;
    private boolean creative;
    private boolean gliding;
    private boolean vehicle;
    private final List<Vec3> velocities = new ArrayList<>();
    private final List<Vec3> drops = new ArrayList<>();
    private final List<Integer> pickupDelays = new ArrayList<>();

    public FakeBody(UUID id, Timeline timeline) {
        this.id = id;
        this.timeline = timeline;
        for (int i = 0; i < STORAGE; i++) {
            storage[i] = "ITEM_" + i;
        }
    }

    public FakeBody holding(String material) {
        mainHand = material;
        return this;
    }

    public FakeBody looking(Vec3 direction) {
        look = direction;
        return this;
    }

    public FakeBody gliding(boolean value) {
        gliding = value;
        return this;
    }

    public FakeBody creative(boolean value) {
        creative = value;
        return this;
    }

    public FakeBody dead(boolean value) {
        dead = value;
        return this;
    }

    public FakeBody mounted(boolean value) {
        vehicle = value;
        return this;
    }

    public String[] storageCopy() {
        return storage.clone();
    }

    public String[] armourCopy() {
        return armour.clone();
    }

    public String offHand() {
        return offHand;
    }

    public String cursor() {
        return cursor;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public Optional<String> mainHand() {
        return Optional.ofNullable(mainHand);
    }

    public List<Vec3> velocities() {
        return velocities;
    }

    public List<Vec3> drops() {
        return drops;
    }

    public List<Integer> pickupDelays() {
        return pickupDelays;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public boolean dead() {
        return dead;
    }

    @Override
    public boolean creativeOrSpectator() {
        return creative;
    }

    @Override
    public boolean gliding() {
        return gliding;
    }

    @Override
    public boolean mounted() {
        return vehicle;
    }

    @Override
    public Vec3 lookDirection() {
        return look;
    }

    @Override
    public int storageSize() {
        return STORAGE;
    }

    @Override
    public void permuteStorage(int[] sourceSlotForSlot) {
        String[] before = storage.clone();
        for (int slot = 0; slot < STORAGE; slot++) {
            storage[slot] = before[sourceSlotForSlot[slot]];
        }
        timeline.note("shuffle");
    }

    @Override
    public Optional<String> mainHandMaterial() {
        return Optional.ofNullable(mainHand);
    }

    @Override
    public void dropMainHand(Vec3 velocity, int pickupDelayTicks) {
        drops.add(velocity);
        pickupDelays.add(pickupDelayTicks);
        mainHand = null;
        timeline.note("drop");
    }

    @Override
    public void pushVelocity(Vec3 velocity) {
        velocities.add(velocity);
        timeline.note("launch");
    }
}
