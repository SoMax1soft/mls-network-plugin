// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public interface RowSink {
    void tickRow(TickRow row);

    void clickRow(ClickRow row);

    void fightStarted();

    void segmentBroken();
}
