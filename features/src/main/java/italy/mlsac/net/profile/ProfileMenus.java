// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.profile;

import java.util.UUID;

@FunctionalInterface
public interface ProfileMenus {

    void openProfile(UUID viewer, String viewerName, String player);
}
