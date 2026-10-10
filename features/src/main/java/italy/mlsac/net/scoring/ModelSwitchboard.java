// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public interface ModelSwitchboard {

    boolean ultraMode();

    boolean enabled(String model);

    boolean onlyAlertConfigured(String model);

    String displayName(String model);
}
