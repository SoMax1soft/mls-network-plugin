// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class CommandGuard implements ConsoleGate {

    private final Supplier<CommandGuardSettings> settings;
    private final RootPolicy roots;
    private final TargetRule targets;
    private final DispatchBudget budget;
    private final GuardReport report;
    private final LongSupplier clock;

    public CommandGuard(Supplier<CommandGuardSettings> settings, RootPolicy roots, TargetRule targets,
                        DispatchBudget budget, GuardReport report, LongSupplier clock) {
        this.settings = settings;
        this.roots = roots;
        this.targets = targets;
        this.budget = budget;
        this.report = report;
        this.clock = clock;
    }

    @Override
    public Optional<String> admit(ConsoleOrder order) {
        CommandGuardSettings current = settings.get();
        if (!current.enabled() || order.command().isBlank()) {
            return Optional.of(order.command());
        }
        Optional<GuardRefusal> shape = CommandShape.refusal(order.command());
        if (shape.isPresent()) {
            return refuse(order, shape.get(), current, Optional.empty());
        }
        CommandLine line = CommandLine.parse(order.command());
        RootVerdict verdict = roots.judge(line, current.extraAllowedRoots());
        if (verdict instanceof RootVerdict.Refused refused) {
            return refuse(order, refused.refusal(), current, Optional.of(line.text()));
        }
        RootVerdict.Cleared cleared = (RootVerdict.Cleared) verdict;
        Optional<GuardRefusal> target = cleared.targetArgument().isPresent()
                ? targets.check(line, cleared.targetArgument().getAsInt(), order.target())
                : Optional.empty();
        if (target.isPresent()) {
            return refuse(order, target.get(), current, Optional.of(line.text()));
        }
        return spend(order, line, cleared.rootKey(), current);
    }

    private Optional<String> spend(ConsoleOrder order, CommandLine line, String rootKey,
                                   CommandGuardSettings current) {
        long now = clock.getAsLong();
        Optional<GuardRefusal> overBudget = budget.check(rootKey, order.target(), now, current);
        if (overBudget.isPresent() && overBudget.get().reason() == GuardReason.BURST) {
            report.burst(rootKey, Integer.parseInt(overBudget.get().detail()), current.burstPauseMinutes(),
                    current.mode());
        } else {
            overBudget.ifPresent(refusal -> report.refused(order, refusal, current.mode()));
        }
        if (overBudget.isPresent() && !current.mode().dispatchesRefused()) {
            return Optional.empty();
        }
        budget.record(rootKey, order.target(), now);
        return Optional.of(line.text());
    }

    private Optional<String> refuse(ConsoleOrder order, GuardRefusal refusal, CommandGuardSettings current,
                                    Optional<String> normalised) {
        report.refused(order, refusal, current.mode());
        if (!current.mode().dispatchesRefused()) {
            return Optional.empty();
        }
        return Optional.of(normalised.orElse(order.command()));
    }
}
