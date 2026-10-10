// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuspectBody;

public final class LaunchTrick implements TrickEffect {

    private final LaunchObserver observer;

    public LaunchTrick(LaunchObserver observer) {
        this.observer = observer;
    }

    @Override
    public TrickType type() {
        return TrickType.LAUNCH;
    }

    @Override
    public boolean perform(SuspectBody body, TrickTuning tuning) {
        if (body.dead() || body.creativeOrSpectator() || body.gliding() || body.mounted()) {
            return false;
        }
        Vec3 velocity = body.lookDirection().normalized().scaled(-tuning.sidewaysPush())
                .withY(tuning.upwardPush());
        body.pushVelocity(velocity);
        observer.launched(body.id());
        return true;
    }
}
