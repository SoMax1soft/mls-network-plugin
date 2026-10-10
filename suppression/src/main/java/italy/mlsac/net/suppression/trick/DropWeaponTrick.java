// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuspectBody;
import java.util.Locale;
import java.util.Optional;

public final class DropWeaponTrick implements TrickEffect {

    public static final int PICKUP_DELAY_TICKS = 40;
    private static final String SWORD_SUFFIX = "_SWORD";

    @Override
    public TrickType type() {
        return TrickType.DROP_WEAPON;
    }

    @Override
    public boolean perform(SuspectBody body, TrickTuning tuning) {
        if (body.dead() || body.creativeOrSpectator()) {
            return false;
        }
        Optional<String> material = body.mainHandMaterial().filter(name -> !name.isBlank());
        if (material.isEmpty()) {
            return false;
        }
        if (tuning.bladeRequired() && !material.get().toUpperCase(Locale.ROOT).endsWith(SWORD_SUFFIX)) {
            return false;
        }
        Vec3 velocity = body.lookDirection().normalized().scaled(tuning.sidewaysPush())
                .withY(tuning.upwardPush());
        body.dropMainHand(velocity, PICKUP_DELAY_TICKS);
        return true;
    }
}
