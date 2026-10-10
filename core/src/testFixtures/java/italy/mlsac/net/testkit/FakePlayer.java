// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.player.PlayerView;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FakePlayer implements PlayerView {
    private static final String BAR_MARK = "[actionbar]";
    private static final String KICK_MARK = "[kick]";

    private final UUID id;
    private final String name;
    private final Set<String> granted = new HashSet<>();
    private final Set<Trait> traits = EnumSet.of(Trait.CONNECTED);
    private final List<String> transcript = new ArrayList<>();

    public FakePlayer(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public FakePlayer grant(String node) {
        granted.add(node);
        return this;
    }

    public FakePlayer asOp() {
        traits.add(Trait.OPERATOR);
        return this;
    }

    public void goOffline() {
        traits.remove(Trait.CONNECTED);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean online() {
        return traits.contains(Trait.CONNECTED);
    }

    @Override
    public boolean hasPermission(String node) {
        return op() || granted.contains(node);
    }

    @Override
    public boolean op() {
        return traits.contains(Trait.OPERATOR);
    }

    @Override
    public void sendMessage(String coloredText) {
        record(coloredText);
    }

    @Override
    public void sendActionBar(String coloredText) {
        record(BAR_MARK + coloredText);
    }

    @Override
    public void kick(String coloredReason) {
        record(KICK_MARK + coloredReason);
        goOffline();
    }

    public synchronized List<String> messages() {
        return List.copyOf(transcript);
    }

    private synchronized void record(String line) {
        transcript.add(line);
    }

    private enum Trait {
        CONNECTED,
        OPERATOR
    }
}
