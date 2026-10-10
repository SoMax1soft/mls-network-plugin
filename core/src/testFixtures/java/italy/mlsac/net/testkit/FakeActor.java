// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.command.CommandActor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FakeActor implements CommandActor {

    private final String name;
    private final UUID player;
    private final Set<String> permissions = new HashSet<>();
    private final List<String> received = new ArrayList<>();
    private boolean everything;

    private FakeActor(String name, UUID player) {
        this.name = name;
        this.player = player;
    }

    public static FakeActor console() {
        FakeActor console = new FakeActor("CONSOLE", null);
        console.everything = true;
        return console;
    }

    public static FakeActor player(String name) {
        return new FakeActor(name, UUID.nameUUIDFromBytes(name.getBytes()));
    }

    public FakeActor grant(String node) {
        permissions.add(node);
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<UUID> actorUuid() {
        return Optional.ofNullable(player);
    }

    @Override
    public boolean hasPermission(String node) {
        return everything || permissions.contains(node);
    }

    @Override
    public boolean op() {
        return everything;
    }

    @Override
    public synchronized void send(String coloredText) {
        received.add(coloredText);
    }

    public synchronized List<String> received() {
        return List.copyOf(received);
    }
}
