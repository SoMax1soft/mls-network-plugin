// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import java.util.UUID;
import java.util.function.Supplier;

public final class MenuDesk {
    private static final String OFFLINE_KEY = "suspects-player-offline";
    private static final String KICKS_OFF_KEY = "menu-kicks-disabled";
    private static final String INVALID_NAME_KEY = "menu-invalid-player";

    private final BlueprintSource blueprints;
    private final Supplier<MenuLayouts> layouts;
    private final FramePainter painter;
    private final MenuCanvas canvas;
    private final MessageBook messages;
    private final Logbook log;
    private final MenuSessions sessions = new MenuSessions();
    private final DossierPages dossierPages;

    public MenuDesk(BlueprintSource blueprints, Supplier<MenuLayouts> layouts, FramePainter painter, MenuCanvas canvas,
                    MessageBook messages, Logbook log, DossierDesk dossiers, Supplier<DossierSettings> dossierSettings,
                    DossierTimes times) {
        this.blueprints = blueprints;
        this.layouts = layouts;
        this.painter = painter;
        this.canvas = canvas;
        this.messages = messages;
        this.log = log;
        this.dossierPages = new DossierPages(dossiers, blueprints, layouts, dossierSettings, painter, canvas, sessions,
                times, log);
    }

    public void openSuspects(UUID viewer, String viewerName) {
        MenuBlueprint blueprint = blueprints.suspects(layouts.get().suspects());
        sessions.hide(viewer);
        canvas.open(viewer, SuspectBoard.loading(blueprint, painter, blueprints.loadingName()));
        long ticket = sessions.awaitRender(viewer);
        canvas.onViewerThread(viewer, () -> fillSuspects(viewer, viewerName, blueprint, ticket));
    }

    public void openChecks(UUID viewer, UUID suspect) {
        canvas.suspect(suspect)
                .map(row -> new ChecksMenu(blueprints.checks(layouts.get().checks()), painter, row,
                        blueprints.emptyChecksName()))
                .ifPresent(menu -> {
                    sessions.show(viewer, menu);
                    canvas.open(viewer, menu.render());
                });
    }

    public void openKicks(UUID viewer, String viewerName, int page) {
        if (!dossierPages.kicksEnabled()) {
            canvas.send(viewer, messages.prefixed(KICKS_OFF_KEY));
            return;
        }
        dossierPages.openKicks(viewer, viewerName, page);
    }

    public void openHistory(UUID viewer, String viewerName, MenuReaction.OpenHistory request) {
        if (!DossierPages.validName(request.player())) {
            canvas.send(viewer, messages.prefixed(INVALID_NAME_KEY));
            return;
        }
        dossierPages.openHistory(viewer, request);
    }

    public void openProfile(UUID viewer, String viewerName, String player) {
        if (!DossierPages.validName(player)) {
            canvas.send(viewer, messages.prefixed(INVALID_NAME_KEY));
            return;
        }
        dossierPages.openProfile(viewer, player);
    }

    public void clicked(UUID viewer, String viewerName, int slot, ClickKind click) {
        sessions.showing(viewer).ifPresent(menu -> menu.click(slot, click)
                .carryOut(new ViewerMenuActions(this, sessions, canvas, messages, log, viewer, viewerName)));
    }

    public boolean owns(UUID viewer) {
        return sessions.involves(viewer);
    }

    public void forget(UUID viewer) {
        sessions.drop(viewer);
    }

    private void fillSuspects(UUID viewer, String viewerName, MenuBlueprint blueprint, long ticket) {
        if (!sessions.finishLoading(viewer, ticket)) {
            return;
        }
        SuspectBoard board = new SuspectBoard(blueprint, painter, viewerName, canvas.onlineSuspects(),
                SuspectPresence.of(canvas::online, messages.text(OFFLINE_KEY)), dossierPages.kicksEnabled());
        sessions.show(viewer, board);
        canvas.redraw(viewer, board.render());
    }
}
