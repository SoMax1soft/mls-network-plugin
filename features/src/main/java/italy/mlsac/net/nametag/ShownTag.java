// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public record ShownTag(TagEntities entities, long lookVersion, TagLines text, double extraHeight) {

    public ShownTag retexted(TagLines replacement) {
        return new ShownTag(entities, lookVersion, replacement, extraHeight);
    }
}
