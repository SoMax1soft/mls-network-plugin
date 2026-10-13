// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

interface BufferBudget {

    boolean claim(RestoreBuffer owner, long bytes);
}
