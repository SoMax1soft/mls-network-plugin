// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.UUID;

public interface ResponseMode {

    void onFlag(FlagEvent flag);

    void onAnswer(BufferAnswer answer);

    void onBufferLowered(UUID suspectId, double buffer);

    void leave();
}
