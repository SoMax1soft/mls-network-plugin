// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.maintenance;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.text.MessageBook;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class ReinstallSubcommand implements Subcommand {

    static final long CONFIRM_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(3);

    private final ReloadDesk desk;
    private final MessageBook messages;
    private final LongSupplier clock;
    private final Map<String, Long> armedAt = new ConcurrentHashMap<>();

    public ReinstallSubcommand(ReloadDesk desk, MessageBook messages, LongSupplier clock) {
        this.desk = desk;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.REINSTALL;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        String who = invoker.actorUuid().map(Object::toString).orElseGet(() -> "#" + invoker.name());
        long now = clock.getAsLong();
        Long armed = armedAt.remove(who);
        if (armed != null && now - armed <= CONFIRM_WINDOW_MILLIS) {
            invoker.send(messages.prefixed(desk.syncConfigAndReload() ? "reinstall-done" : "reinstall-failed"));
            return;
        }
        armedAt.put(who, now);
        invoker.send(messages.prefixed("reinstall-confirm"));
    }
}
