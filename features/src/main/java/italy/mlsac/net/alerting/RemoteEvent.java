// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.Optional;
import java.util.OptionalDouble;

public record RemoteEvent(String eventId, Optional<String> type, Optional<String> model, Optional<String> action,
                          Optional<String> hostLabel, Optional<String> playerNick,
                          OptionalDouble offenceLevel, OptionalDouble vl, double probability, double buffer,
                          boolean carriesReportFields) {
}
