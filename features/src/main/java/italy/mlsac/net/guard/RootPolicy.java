// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;
import java.util.Set;

public final class RootPolicy {

    private static final int FIRST_ARGUMENT = 0;
    private static final int SECOND_ARGUMENT = 1;

    private final CommandRoots roots;

    public RootPolicy(CommandRoots roots) {
        this.roots = roots;
    }

    public RootVerdict judge(CommandLine line, Set<String> extraAllowed) {
        if (line.bare().isEmpty()) {
            return RootVerdict.refused(GuardReason.NOT_ALLOWED, line.label());
        }
        if (line.bare().startsWith("/")) {
            return RootVerdict.refused(GuardReason.WORLDEDIT, line.label());
        }
        if (RootCatalog.denied(line.bare())) {
            return RootVerdict.refused(GuardReason.DENIED_ROOT, line.bare());
        }
        if (line.namespace().filter(RootCatalog::deniedOwner).isPresent()) {
            return RootVerdict.refused(GuardReason.DENIED_OWNER, line.namespace().get());
        }
        Optional<RootOwner> owner = roots.resolve(line.label());
        if (owner.isPresent() && RootCatalog.denied(owner.get().command())) {
            return RootVerdict.refused(GuardReason.DENIED_ROOT, owner.get().command());
        }
        if (owner.isPresent() && RootCatalog.deniedOwner(owner.get().ownerKey())) {
            return RootVerdict.refused(GuardReason.DENIED_OWNER, owner.get().owner());
        }
        String rootKey = owner.map(RootOwner::command).filter(command -> !command.isEmpty()).orElse(line.bare());
        if (listedByOwner(line, rootKey, extraAllowed)) {
            return extra(line, rootKey);
        }
        if (owner.isEmpty()) {
            return RootVerdict.refused(GuardReason.UNKNOWN_COMMAND, line.label());
        }
        return byOwner(line, owner.get(), rootKey);
    }

    private static boolean listedByOwner(CommandLine line, String rootKey, Set<String> extraAllowed) {
        return extraAllowed.contains(line.label()) || extraAllowed.contains(line.bare())
                || extraAllowed.contains(rootKey);
    }

    private static RootVerdict extra(CommandLine line, String rootKey) {
        if (RootCatalog.punishmentVerb(rootKey) || RootCatalog.punishmentVerb(line.bare())) {
            return RootVerdict.targetAt(rootKey, FIRST_ARGUMENT);
        }
        Optional<String> sub = line.firstArgumentLowered().filter(RootCatalog::denied);
        if (sub.isPresent()) {
            return RootVerdict.refused(GuardReason.DENIED_ROOT, line.label() + " " + sub.get());
        }
        return RootVerdict.untargeted(rootKey);
    }

    private static RootVerdict byOwner(CommandLine line, RootOwner owner, String rootKey) {
        String ownerKey = owner.ownerKey();
        String command = owner.command();
        boolean vanilla = ownerKey.equals(RootOwner.MINECRAFT) || ownerKey.equals(RootOwner.SERVER);
        if (vanilla && RootCatalog.VANILLA_PUNISHMENTS.contains(command)) {
            return RootVerdict.targetAt(rootKey, FIRST_ARGUMENT);
        }
        if (RootCatalog.pluginPunishments(ownerKey).filter(verbs -> verbs.contains(command)).isPresent()) {
            return RootVerdict.targetAt(rootKey, FIRST_ARGUMENT);
        }
        if (command.equals(RootCatalog.SUBCOMMAND_ROOTS.get(ownerKey))) {
            return subcommand(line, ownerKey);
        }
        if (ownerKey.equals(RootCatalog.OWN_PLUGIN) && command.equals(RootCatalog.OWN_COMMAND)) {
            return own(line, rootKey);
        }
        if (RootCatalog.FEEDBACK.contains(command)) {
            return RootVerdict.untargeted(rootKey);
        }
        return RootVerdict.refused(GuardReason.NOT_ALLOWED, line.label());
    }

    private static RootVerdict subcommand(CommandLine line, String ownerKey) {
        Optional<String> sub = line.firstArgumentLowered();
        Set<String> verbs = RootCatalog.pluginPunishments(ownerKey).orElse(Set.of());
        if (sub.isPresent() && verbs.contains(sub.get())) {
            return RootVerdict.targetAt(sub.get(), SECOND_ARGUMENT);
        }
        return RootVerdict.refused(GuardReason.NOT_ALLOWED, line.label() + sub.map(word -> " " + word).orElse(""));
    }

    private static RootVerdict own(CommandLine line, String rootKey) {
        Optional<String> sub = line.firstArgumentLowered();
        if (sub.isEmpty()) {
            return RootVerdict.untargeted(rootKey);
        }
        if (RootCatalog.OWN_DENIED_SUBCOMMANDS.contains(sub.get())) {
            return RootVerdict.refused(GuardReason.NOT_ALLOWED, line.label() + " " + sub.get());
        }
        if (sub.get().equals(RootCatalog.OWN_PUNISH)) {
            return RootVerdict.targetAt(rootKey + " " + sub.get(), SECOND_ARGUMENT);
        }
        return RootVerdict.untargeted(rootKey + " " + sub.get());
    }
}
