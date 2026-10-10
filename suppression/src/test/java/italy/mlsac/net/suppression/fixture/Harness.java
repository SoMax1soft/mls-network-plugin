// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.SuppressionKit;
import italy.mlsac.net.suppression.SuppressionEngine;
import italy.mlsac.net.suppression.SuppressionPorts;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.standing.EffectReadout;
import italy.mlsac.net.suppression.trigger.AnswerKind;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.BufferTrigger;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public final class Harness {

    public static final UUID STEVE = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000016fb1");
    public static final String STEVE_NAME = "Steve";

    public final ManualClock clock = new ManualClock();
    public final ScriptedRandom random = new ScriptedRandom(0.5);
    public final Timeline timeline = new Timeline(clock);
    public final FakeServer server = new FakeServer(timeline);
    public final RecordingLedger ledger = new RecordingLedger();
    public final SuppressionKit<String> assembly;

    private double currentBuffer;

    public Harness(SuppressionPlan plan) {
        SuppressionPorts ports = new SuppressionPorts(clock, clock, random, server, server, server, server, server,
                server, server, server, server, server, ledger);
        this.assembly = new SuppressionKit<>(ports, plan);
    }

    public SuppressionEngine engine() {
        return assembly.engine();
    }

    public EffectReadout readout() {
        return assembly.readout();
    }

    public void at(long millis) {
        clock.set(millis);
    }

    public void tickAt(long millis) {
        clock.set(millis);
        engine().tick();
    }

    public TriggerContext context(int vl, double probability, double buffer) {
        return new TriggerContext(STEVE, STEVE_NAME, vl, probability, buffer);
    }

    public void flag(int vl) {
        flag(vl, 0.97, 20.0);
    }

    public void flag(int vl, double probability, double buffer) {
        engine().onFlag(new FlagEvent(context(vl, probability, buffer), SuspectOrigin.PLAYER));
    }

    public Optional<BufferTrigger> answer(double buffer) {
        return answer(buffer, 0.95);
    }

    public Optional<BufferTrigger> answer(double buffer, double probability) {
        currentBuffer = buffer;
        return engine().onAnswer(new BufferAnswer(context(0, probability, buffer), AnswerKind.SCORING,
                SuspectOrigin.PLAYER));
    }

    public void flagReset(double resetValue) {
        currentBuffer = resetValue;
        engine().onBufferLowered(STEVE, resetValue);
    }

    public void applyPlan(SuppressionPlan plan) {
        engine().applyPlan(plan, id -> OptionalDouble.of(currentBuffer));
    }

    public void quit() {
        server.goOffline(STEVE);
        engine().quitSeen(STEVE);
    }

    public void join() {
        server.goOnline(STEVE);
        engine().onJoin(STEVE);
    }

    public int dealt() {
        return readout().dealtPercent(STEVE);
    }

    public int taken() {
        return readout().takenPercent(STEVE);
    }
}
