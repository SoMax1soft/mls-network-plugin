// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import italy.mlsac.net.player.GrantNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum SubcommandName {
    START(false, "collect", "collect"),
    STOP(false, "collect", "collect"),
    ALERTS(true, "alerts", "alerts"),
    MONITOR(true, "monitor", "alerts"),
    SUSPECTS(true, "suspects", "alerts"),
    PROB(true, "profile", "prob"),
    PROFILE(false, "profile", "prob"),
    RELOAD(false, "reload", "reload"),
    PUNISH(false, "punish"),
    KICKLIST(false, "kicklist"),
    FALSEPOSITIVE(false, "falsepositive"),
    STATUS(false, "status"),
    DATASTATUS(false, "status"),
    REINSTALL(false, "reinstall"),
    ANIMATION(false, "animation");

    private final boolean playerOnly;
    private final List<GrantNode> grants;

    SubcommandName(boolean playerOnly, String commandVerb, String... olderNames) {
        this.playerOnly = playerOnly;
        List<GrantNode> accepted = new ArrayList<>();
        accepted.add(GrantNode.forCommand(commandVerb));
        for (String older : olderNames) {
            accepted.add(GrantNode.older(older));
        }
        accepted.add(GrantNode.OPERATOR);
        this.grants = List.copyOf(accepted);
    }

    public static Optional<SubcommandName> parse(String raw) {
        String id = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        for (SubcommandName name : values()) {
            if (name.name().equals(id)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean playerOnly() {
        return playerOnly;
    }

    public List<GrantNode> grants() {
        return grants;
    }

    public boolean permits(CommandActor actor) {
        return grants.stream().map(GrantNode::node).anyMatch(actor::hasPermission);
    }
}
