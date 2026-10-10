// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.UUID;

public interface DeliveryLane {

    void onPlayerThread(UUID player, Runnable task);

    void onMainThread(Runnable task);
}
