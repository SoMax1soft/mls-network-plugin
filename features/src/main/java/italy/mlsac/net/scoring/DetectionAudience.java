// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.List;

public interface DetectionAudience {

    void answerSeen(SuspectRecord suspect, ScreenedVerdict verdict);

    void stepsCrossed(SuspectRecord suspect, ScreenedVerdict verdict, List<AlertStep> steps, double buffer);

    void flagRaised(FlagContext flag);
}
