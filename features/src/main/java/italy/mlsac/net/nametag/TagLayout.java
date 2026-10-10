// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public record TagLayout(double extraHeight, OwnTag ownTag) {

    static final double LOWEST = -1.0;
    static final double HIGHEST = 3.0;

    public TagLayout {
        extraHeight = Double.isFinite(extraHeight) ? Math.max(LOWEST, Math.min(HIGHEST, extraHeight)) : 0.0;
    }

    public static TagLayout stock() {
        return new TagLayout(0.0, OwnTag.HIDDEN);
    }
}
