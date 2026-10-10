// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public record NametagLook(NametagSettings settings, long version) {

    public boolean enabled() {
        return settings.enabled();
    }

    public boolean ownTagShown() {
        return settings.layout().ownTag().shown();
    }
}
