// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

public enum GuardReason {
    CONTROL_CHARACTER("control-character", "contains a line break or control character", true),
    TOO_LONG("too-long", "longer than " + CommandShape.MAX_LENGTH + " characters", true),
    CHAINING("chaining", "chains commands (; && || |)", true),
    SELECTOR("selector", "uses the target selector {DETAIL}", true),
    WORLDEDIT("worldedit", "is a WorldEdit command", true),
    DENIED_ROOT("denied-root", "'{DETAIL}' is never allowed", true),
    DENIED_OWNER("denied-owner", "belongs to {DETAIL}, which is never allowed", true),
    NOT_ALLOWED("not-allowed", "'{DETAIL}' is not allowed (add it to command-guard.extra-allowed-roots in config.yml)",
            true),
    UNKNOWN_COMMAND("unknown-command", "'{DETAIL}' is not a command on this server", false),
    WRONG_TARGET("wrong-target", "the target is not the punished player ({DETAIL})", true),
    UNSAFE_NAME("unsafe-name", "player name {DETAIL} is not a plain Minecraft name", true),
    RATE_LIMIT("rate-limit", "more than {DETAIL} site commands per minute", true),
    PLAYER_RATE_LIMIT("player-rate-limit", "more than {DETAIL} site commands per minute for one player", true),
    BURST("burst", "the same command went to too many players", true),
    PAUSED("paused", "site commands are paused after a burst", true);

    public static final String DETAIL = "{DETAIL}";

    private final String id;
    private final String english;
    private final boolean alertsStaff;

    GuardReason(String id, String english, boolean alertsStaff) {
        this.id = id;
        this.english = english;
        this.alertsStaff = alertsStaff;
    }

    public String id() {
        return id;
    }

    public String english() {
        return english;
    }

    public boolean alertsStaff() {
        return alertsStaff;
    }
}
