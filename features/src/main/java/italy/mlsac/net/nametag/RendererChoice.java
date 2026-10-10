// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.Optional;

public final class RendererChoice {

    private final GameRelease server;
    private final OverheadTagPainter textDisplays;
    private final OverheadTagPainter armorStands;

    public RendererChoice(GameRelease server) {
        this.server = server;
        this.textDisplays = new TextDisplayRenderer(server);
        this.armorStands = new ArmorStandRenderer(server);
    }

    public OverheadTagPainter forViewer(Optional<GameRelease> client) {
        boolean clientSupports = client.map(release -> release.atLeast(GameRelease.TEXT_DISPLAYS)).orElse(true);
        return server.atLeast(GameRelease.TEXT_DISPLAYS) && clientSupports ? textDisplays : armorStands;
    }

    public TagDrawing resolve(TagDrawing wanted, Optional<GameRelease> client) {
        return switch (wanted) {
            case AUTO -> forViewer(client) == textDisplays ? TagDrawing.DISPLAY : TagDrawing.STAND;
            case DISPLAY -> server.atLeast(GameRelease.TEXT_DISPLAYS) ? TagDrawing.DISPLAY : TagDrawing.STAND;
            case STAND -> TagDrawing.STAND;
        };
    }

    public OverheadTagPainter painter(TagDrawing resolved) {
        return resolved == TagDrawing.DISPLAY ? textDisplays : armorStands;
    }
}
