// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRoster {

    Optional<StaffMember> find(UUID player);

    List<StaffMember> online();
}
