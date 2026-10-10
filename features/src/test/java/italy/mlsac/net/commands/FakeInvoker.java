// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import italy.mlsac.net.command.CommandActor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FakeInvoker implements CommandActor {

    public final List<String> received = new ArrayList<>();
    private final String name;
    private final Optional<UUID> id;
    private final Set<String> nodes;

    private FakeInvoker(String name, Optional<UUID> id, Set<String> nodes) {
        this.name = name;
        this.id = id;
        this.nodes = nodes;
    }

    public static FakeInvoker player(String name, String... nodes) {
        return new FakeInvoker(name, Optional.of(UUID.nameUUIDFromBytes(name.getBytes())), Set.of(nodes));
    }

    public static FakeInvoker console() {
        return new FakeInvoker("CONSOLE", Optional.empty(), Set.of("mlsac.admin"));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<UUID> actorUuid() {
        return id;
    }

    @Override
    public boolean hasPermission(String node) {
        return nodes.contains(node);
    }

    @Override
    public boolean op() {
        return false;
    }

    @Override
    public void send(String coloredText) {
        received.add(coloredText);
    }

    public String last() {
        return received.get(received.size() - 1);
    }
}
