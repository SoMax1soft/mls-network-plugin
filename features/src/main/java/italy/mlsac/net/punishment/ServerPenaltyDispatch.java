// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ServerPenaltyDispatch implements PenaltyDispatch {

    private final PenaltyServer server;
    private final AnimationStage animations;
    private final StaffNotices staff;
    private final MessageBook messages;
    private final Supplier<PenaltyRules> rules;
    private final Logbook log;

    public ServerPenaltyDispatch(PenaltyServer server, AnimationStage animations, StaffNotices staff,
                                 MessageBook messages, Supplier<PenaltyRules> rules, Logbook log) {
        this.server = server;
        this.animations = animations;
        this.staff = staff;
        this.messages = messages;
        this.rules = rules;
        this.log = log;
    }

    @Override
    public void console(String resolvedCommand) {
        server.dispatchConsoleNextTick(resolvedCommand);
    }

    @Override
    public void staffNotice(String resolvedText) {
        String raw = messages.rawPrefix() + resolvedText;
        staff.toAllStaff(messages.colorize(raw));
        if (rules.get().alertsConsole()) {
            log.info(messages.strip(raw));
        }
    }

    @Override
    public void animated(UUID player, String resolvedCommand, Optional<String> animationId) {
        PenaltyRules current = rules.get();
        if (!current.banShowOn() || !server.online(player)) {
            server.dispatchConsoleNextTick(resolvedCommand);
            return;
        }
        String wanted = animationId.filter(animations::knows).orElseGet(current::animationOrDefault);
        if (!animations.knows(wanted)) {
            log.warn("[Penalty] Animation '" + wanted + "' not found, using " + PenaltyRules.DEFAULT_ANIMATION);
            wanted = PenaltyRules.DEFAULT_ANIMATION;
        }
        if (!animations.knows(wanted)) {
            log.warn("[Penalty] Animation " + PenaltyRules.DEFAULT_ANIMATION
                    + " is missing, running the command without animation");
            server.dispatchConsoleNextTick(resolvedCommand);
            return;
        }
        animations.play(player, wanted, resolvedCommand);
    }
}
