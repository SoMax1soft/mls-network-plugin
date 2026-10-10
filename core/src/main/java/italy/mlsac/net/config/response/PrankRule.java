// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.response;

public record PrankRule(TrickKind kind, double buffer, boolean enabled, int restSeconds, boolean bladeRequired,
                          double sidewaysPush, double upwardPush, String message) {

    public static PrankRule defaultsFor(TrickKind kind) {
        return new PrankRule(kind, 0, true, 0, true, kind.defaultHorizontal(), kind.defaultVertical(), "");
    }
}
