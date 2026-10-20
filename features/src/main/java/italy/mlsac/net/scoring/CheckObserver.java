// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

@FunctionalInterface
public interface CheckObserver {

    CheckObserver NONE = outcome -> {
    };

    void checked(CheckOutcome outcome);
}
