// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

public enum MenuId {
    SUSPECTS("suspects"),
    CHECKS("checks"),
    KICKS("kicks"),
    PROFILE("profile");

    private final String id;

    MenuId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
