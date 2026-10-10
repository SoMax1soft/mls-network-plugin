// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public interface LegacyWindowLength {

    int current();

    void change(int newLength);
}
