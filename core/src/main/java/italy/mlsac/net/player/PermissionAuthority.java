// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

@FunctionalInterface
public interface PermissionAuthority {

    PermissionAuthority ALWAYS = () -> true;

    boolean installed();
}
