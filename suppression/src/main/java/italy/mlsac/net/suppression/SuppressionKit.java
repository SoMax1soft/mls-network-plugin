// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.suppression.chain.ChainBoard;
import italy.mlsac.net.suppression.damage.DamageCauses;
import italy.mlsac.net.suppression.damage.DamageSuppression;
import italy.mlsac.net.suppression.damage.FinalDamageScaler;
import italy.mlsac.net.suppression.execution.ActionScene;
import italy.mlsac.net.suppression.execution.BufferTriggerRunner;
import italy.mlsac.net.suppression.execution.CommandStage;
import italy.mlsac.net.suppression.execution.CurrentPlan;
import italy.mlsac.net.suppression.execution.InGameApplier;
import italy.mlsac.net.suppression.execution.PunishmentCooldown;
import italy.mlsac.net.suppression.execution.StageRunner;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.hit.MissLag;
import italy.mlsac.net.suppression.lag.motion.GroundLag;
import italy.mlsac.net.suppression.lag.motion.GroundPulseRule;
import italy.mlsac.net.suppression.lag.motion.RotateLag;
import italy.mlsac.net.suppression.lag.motion.SetbackLag;
import italy.mlsac.net.suppression.lag.motion.TurnRoll;
import italy.mlsac.net.suppression.lag.packet.DelayLag;
import italy.mlsac.net.suppression.lag.packet.HeldDisposal;
import italy.mlsac.net.suppression.lag.runtime.CombatLedger;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.lag.runtime.LagTicker;
import italy.mlsac.net.suppression.notice.StaffLines;
import italy.mlsac.net.suppression.notice.StockTrickTexts;
import italy.mlsac.net.suppression.notice.TrickBroadcast;
import italy.mlsac.net.suppression.notice.TrickTextChoice;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.standing.CachedExemption;
import italy.mlsac.net.suppression.standing.EffectReadout;
import italy.mlsac.net.suppression.standing.SuspectStanding;
import italy.mlsac.net.suppression.suspect.RetentionPolicy;
import italy.mlsac.net.suppression.suspect.SuspectRegistry;
import italy.mlsac.net.suppression.trick.DropWeaponTrick;
import italy.mlsac.net.suppression.trick.LaunchTrick;
import italy.mlsac.net.suppression.trick.ShuffleInventoryTrick;
import italy.mlsac.net.suppression.trick.StoragePermutation;
import italy.mlsac.net.suppression.trick.TrickEffects;
import java.util.List;

public final class SuppressionKit<P> {

    private final SuppressionEngine engine;
    private final EffectReadout readout;
    private final DamageSuppression damage;
    private final DelayLag<P> delay;
    private final LagTicker ticker;
    private final LagRuntimes<P> lagRuntimes;
    private final CombatLedger combat;

    public SuppressionKit(SuppressionPorts ports, SuppressionPlan initialPlan) {
        this(ports, initialPlan, HeldDisposal.forgetting());
    }

    public SuppressionKit(SuppressionPorts ports, SuppressionPlan initialPlan, HeldDisposal<P> heldDisposal) {
        CurrentPlan plan = new CurrentPlan(initialPlan);
        SuspectRegistry registry = new SuspectRegistry(RetentionPolicy.standard());
        SuspectStanding standing = new SuspectStanding(ports.status(),
                new CachedExemption(ports.exemption(), ports.clock()), ports.lagExempt());
        this.readout = new EffectReadout(registry, standing, ports.clock());
        this.lagRuntimes = new LagRuntimes<>(heldDisposal);
        this.combat = new CombatLedger();
        LagTable table = LagTable.standard();
        TrickEffects tricks = new TrickEffects(List.of(
                new ShuffleInventoryTrick(new StoragePermutation(ports.random())),
                new DropWeaponTrick(),
                new LaunchTrick(lagRuntimes)));
        TrickBroadcast broadcast = new TrickBroadcast(
                new TrickTextChoice(ports.texts(), StockTrickTexts.legacyEditorTexts()), ports.notices());
        ActionScene scene = new ActionScene(registry, standing, ports.bodies(), tricks, broadcast, ports.watcher());
        InGameApplier applier = new InGameApplier(scene, plan, ports.clock());
        StaffLines lines = new StaffLines(ports.texts(), ports.notices());
        PunishmentCooldown cooldown = new PunishmentCooldown(ports.wallClock());
        CommandStage commands = new CommandStage(ports.commands(), cooldown, ports.notices());
        StageRunner stages = new StageRunner(ports.threads(), applier, commands, lines, plan, ports.ledger());
        BufferTriggerRunner bufferTriggers = new BufferTriggerRunner(ports.threads(), applier, lines, plan,
                ports.ledger());
        SuspectExits exits = new SuspectExits(lagRuntimes, readout::lagLevels, combat, standing, cooldown);
        this.engine = new SuppressionEngine(new EngineParts(plan, registry, new ChainBoard(), stages, bufferTriggers,
                exits, ports.watcher(), ports.notices(), ports.clock()));
        MissLag miss = new MissLag(readout::lagLevels, lagRuntimes, table, ports.random());
        this.damage = new DamageSuppression(readout, miss, DamageCauses.standard(), new FinalDamageScaler());
        this.delay = new DelayLag<>(readout::lagLevels, standing::lagExempt, lagRuntimes, table, ports.random());
        this.ticker = new LagTicker(readout::lagLevels, lagRuntimes, combat, List.of(
                new SetbackLag(table, ports.random(), ports.terrain()),
                new GroundLag(table, new GroundPulseRule()),
                new RotateLag(table, ports.random(), new TurnRoll(ports.random()))));
    }

    public SuppressionEngine engine() {
        return engine;
    }

    public EffectReadout readout() {
        return readout;
    }

    public DamageSuppression damage() {
        return damage;
    }

    public DelayLag<P> delay() {
        return delay;
    }

    public LagTicker ticker() {
        return ticker;
    }

    public LagRuntimes<P> lagRuntimes() {
        return lagRuntimes;
    }

    public CombatLedger combat() {
        return combat;
    }
}
