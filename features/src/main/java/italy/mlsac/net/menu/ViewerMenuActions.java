// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import java.util.UUID;

final class ViewerMenuActions implements MenuActions, ButtonOutcomes {
    private final MenuDesk desk;
    private final MenuSessions sessions;
    private final MenuCanvas canvas;
    private final MessageBook messages;
    private final Logbook log;
    private final UUID viewer;
    private final String viewerName;

    ViewerMenuActions(MenuDesk desk, MenuSessions sessions, MenuCanvas canvas, MessageBook messages, Logbook log,
            UUID viewer, String viewerName) {
        this.desk = desk;
        this.sessions = sessions;
        this.canvas = canvas;
        this.messages = messages;
        this.log = log;
        this.viewer = viewer;
        this.viewerName = viewerName;
    }

    @Override
    public void redraw(MenuFrame frame) {
        canvas.redraw(viewer, frame);
    }

    @Override
    public void close() {
        sessions.hide(viewer);
        canvas.close(viewer);
    }

    @Override
    public void runButton(ButtonCommand command) {
        command.deliverTo(this);
    }

    @Override
    public void refused() {
        canvas.send(viewer, messages.text(ButtonCommand.REFUSAL_KEY));
    }

    @Override
    public void dispatch(String command) {
        canvas.runConsoleNextTick(command);
        log.info("[Menu] button pressed by " + viewerName + " dispatched: " + command);
    }

    @Override
    public void openChecks(UUID suspect) {
        desk.openChecks(viewer, suspect);
    }

    @Override
    public void openSuspects() {
        desk.openSuspects(viewer, viewerName);
    }

    @Override
    public void teleport(UUID suspect) {
        canvas.teleport(viewer, suspect);
    }

    @Override
    public void tell(String coloredMessage) {
        canvas.send(viewer, coloredMessage);
    }

    @Override
    public void openKicks(int page) {
        desk.openKicks(viewer, viewerName, page);
    }

    @Override
    public void openHistory(MenuReaction.OpenHistory request) {
        desk.openHistory(viewer, viewerName, request);
    }

    @Override
    public void openProfile(String player) {
        desk.openProfile(viewer, viewerName, player);
    }
}
