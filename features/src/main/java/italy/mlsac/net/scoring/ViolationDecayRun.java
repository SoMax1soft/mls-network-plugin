// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.function.Supplier;

public final class ViolationDecayRun implements Runnable {

    private final ViolationLedger ledger;
    private final Supplier<ViolationDecayRules> rules;
    private final CombatProbe combat;

    public ViolationDecayRun(ViolationLedger ledger, Supplier<ViolationDecayRules> rules, CombatProbe combat) {
        this.ledger = ledger;
        this.rules = rules;
        this.combat = combat;
    }

    @Override
    public void run() {
        ViolationDecayRules current = rules.get();
        if (current.enabled()) {
            ledger.decay(current.amount(), combat::inLegacyCombat);
        }
    }
}
