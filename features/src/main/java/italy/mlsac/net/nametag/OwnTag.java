// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public enum OwnTag {
    HIDDEN,
    SHOWN;

    public static OwnTag of(boolean showSelf) {
        return showSelf ? SHOWN : HIDDEN;
    }

    public boolean shown() {
        return this == SHOWN;
    }
}
