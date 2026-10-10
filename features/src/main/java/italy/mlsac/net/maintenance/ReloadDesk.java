// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.maintenance;

public interface ReloadDesk {

    void scheduleFullReload();

    int reloadAnimations();

    boolean syncConfigAndReload();
}
