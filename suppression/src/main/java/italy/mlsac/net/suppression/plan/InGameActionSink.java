// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public interface InGameActionSink {

    void timed(TimedAction action);

    void trick(TrickAction action);
}
