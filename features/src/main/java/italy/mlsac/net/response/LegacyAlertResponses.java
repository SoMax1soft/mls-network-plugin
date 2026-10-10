// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.text.DisplayNumbers;
import italy.mlsac.net.text.TemplateValues;
import italy.mlsac.net.scoring.AnswerMoment;
import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.PlayerThread;
import italy.mlsac.net.scoring.SuspectRecord;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class LegacyAlertResponses implements BufferReactions {

    private final Supplier<ResponseRules> rules;
    private final PlayerThread players;
    private final DamageReductions reductions;
    private final TrickCooldowns cooldowns;
    private final TrickHands hands;
    private final TrickAnnouncer announcer;
    private final LegacyColorizer colors;
    private final LongSupplier clock;
    private final Logbook log;

    public LegacyAlertResponses(Supplier<ResponseRules> rules, PlayerThread players, DamageReductions reductions,
                                TrickCooldowns cooldowns, TrickHands hands, TrickAnnouncer announcer,
                                LegacyColorizer colors, LongSupplier clock, Logbook log) {
        this.rules = rules;
        this.players = players;
        this.reductions = reductions;
        this.cooldowns = cooldowns;
        this.hands = hands;
        this.announcer = announcer;
        this.colors = colors;
        this.clock = clock;
        this.log = log;
    }

    @Override
    public void bufferMoved(AnswerMoment moment) {
    }

    @Override
    public void answerAccepted(AnswerMoment moment) {
        SuspectRecord suspect = moment.suspect();
        double probability = moment.verdict().probability();
        players.runNextTick(suspect.id(), () -> {
            if (players.online(suspect.id())) {
                evaluate(suspect, probability);
            }
        });
    }

    @Override
    public void bufferReset(SuspectRecord suspect, double buffer) {
    }

    public void evaluate(SuspectRecord suspect, double probability) {
        ResponseRules current = rules.get();
        if (!current.enabled()) {
            return;
        }
        double buffer = suspect.buffer();
        long now = clock.getAsLong();
        if (current.damageEnabled()) {
            for (ReductionStage stage : current.stages()) {
                if (stage.usable() && suspect.latches().crossed(stage.latchName(), buffer, stage.buffer())) {
                    reductions.offer(suspect.id(), stage.clampedPercent(), now + stage.lastsSeconds() * 1000L, now);
                }
            }
        }
        if (current.pranksOn()) {
            for (TrollTrick trick : current.tricks()) {
                if (trick.usable() && suspect.latches().crossed(trick.latchName(), buffer, trick.buffer())) {
                    perform(suspect, trick, buffer, probability, now);
                }
            }
        }
    }

    private void perform(SuspectRecord suspect, TrollTrick trick, double buffer, double probability, long now) {
        if (!cooldowns.ready(suspect.id(), trick.type(), now)) {
            return;
        }
        Optional<TrickMove> move = TrickMove.of(trick.type());
        if (move.isEmpty()) {
            log.warn("[Responses] Unknown troll action type: " + trick.type());
            return;
        }
        if (!move.get().perform(hands, suspect.id(), trick)) {
            return;
        }
        cooldowns.start(suspect.id(), trick.type(), trick.restSeconds(), now);
        if (!trick.message().isBlank()) {
            String buffered = DisplayNumbers.oneDecimal(buffer);
            announcer.toStaffOrOperators(colors.colorize(TemplateValues.of("{PLAYER}", suspect.name())
                    .and("{BUFFER}", buffered)
                    .and("{DETECTIONS}", buffered)
                    .and("{PROBABILITY}", DisplayNumbers.twoDecimals(probability))
                    .apply(trick.message())));
        }
    }

    public void configurationChanged() {
        reductions.clear();
        cooldowns.clear();
    }
}
