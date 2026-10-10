// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public enum SuspectKind {
    JAVA,
    BEDROCK;

    public boolean scored() {
        return this == JAVA;
    }
}
