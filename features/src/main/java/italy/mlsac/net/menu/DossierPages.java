// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.backend.dossier.DossierKind;
import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierReading;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.TemplateValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class DossierPages {

    static final Pattern PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_.*-]{1,32}$");
    static final int MAX_PAGE_SIZE = 45;
    static final String LOADING_MATERIAL = "SUNFLOWER";
    static final String PUNISH_KIND = "punish";
    static final String PUNISH_AND_SUPPRESSION_KINDS = "punish,suppression";

    private final DossierDesk dossiers;
    private final BlueprintSource blueprints;
    private final Supplier<MenuLayouts> layouts;
    private final Supplier<DossierSettings> settings;
    private final FramePainter painter;
    private final MenuCanvas canvas;
    private final MenuSessions sessions;
    private final DossierTimes times;
    private final Logbook log;

    DossierPages(DossierDesk dossiers, BlueprintSource blueprints, Supplier<MenuLayouts> layouts,
                 Supplier<DossierSettings> settings, FramePainter painter, MenuCanvas canvas, MenuSessions sessions,
                 DossierTimes times, Logbook log) {
        this.dossiers = dossiers;
        this.blueprints = blueprints;
        this.layouts = layouts;
        this.settings = settings;
        this.painter = painter;
        this.canvas = canvas;
        this.sessions = sessions;
        this.times = times;
        this.log = log;
    }

    static boolean validName(String player) {
        return player != null && PLAYER_NAME.matcher(player).matches();
    }

    boolean kicksEnabled() {
        return settings.get().kicks().enabled();
    }

    void openKicks(UUID viewer, String viewerName, int page) {
        DossierSettings now = settings.get();
        MenuBlueprint blueprint = blueprints.kicks(layouts.get().kicks());
        DossierQuery query = DossierQuery.of(DossierKind.KICKS)
                .with("page", Math.max(0, page))
                .with("size", Math.min(MAX_PAGE_SIZE, blueprint.slotsPerSheet()))
                .with("days", now.kicks().days())
                .with("bans", now.kicks().bans() ? 1 : 0)
                .with("scope", now.scope());
        load(viewer, blueprint, TemplateValues.of("{PLAYER}", viewerName), query, new MenuReaction.OpenSuspects(),
                data -> new KicksBoard(blueprint, painter, DossierReading.kicks(data), times,
                        blueprints.emptyKicksName(), now.kicks().days(), now.kicks().suppressions(), viewerName));
    }

    void openHistory(UUID viewer, MenuReaction.OpenHistory request) {
        DossierSettings now = settings.get();
        MenuBlueprint blueprint = blueprints.history();
        DossierQuery query = DossierQuery.of(DossierKind.HISTORY)
                .with("player", request.player())
                .with("page", Math.max(0, request.page()))
                .with("size", Math.min(MAX_PAGE_SIZE, blueprint.slotsPerSheet()))
                .with("kinds", request.withSuppressions() ? PUNISH_AND_SUPPRESSION_KINDS : PUNISH_KIND)
                .with("scope", now.scope());
        HistoryTexts texts = blueprints.historyTexts();
        load(viewer, blueprint, TemplateValues.of("{PLAYER}", request.player()), query, request.back(),
                data -> new HistoryBoard(blueprint, painter, DossierReading.history(data), texts, times, request));
    }

    void openProfile(UUID viewer, String player) {
        DossierSettings now = settings.get();
        MenuBlueprint blueprint = blueprints.profile(layouts.get().profile());
        DossierQuery query = DossierQuery.of(DossierKind.PROFILE)
                .with("player", player)
                .with("scope", now.scope())
                .with("latest", now.profile().latest());
        ProfileTexts texts = blueprints.profileTexts();
        load(viewer, blueprint, TemplateValues.of("{PLAYER}", player), query, new MenuReaction.Close(),
                data -> new ProfileBoard(blueprint, painter, DossierReading.profile(data), texts, times,
                        now.profile(), player));
    }

    private void load(UUID viewer, MenuBlueprint blueprint, TemplateValues titleValues, DossierQuery query,
                      MenuReaction back, Function<JsonObject, OpenMenu> board) {
        sessions.hide(viewer);
        canvas.open(viewer, loading(blueprint, titleValues));
        long ticket = sessions.awaitRender(viewer);
        dossiers.ask(viewer, query).thenAccept(reply -> canvas.onViewerThread(viewer,
                () -> finish(viewer, ticket, blueprint, titleValues, reply, back, board)));
    }

    private void finish(UUID viewer, long ticket, MenuBlueprint blueprint, TemplateValues titleValues,
                        DossierReply reply, MenuReaction back, Function<JsonObject, OpenMenu> board) {
        if (!sessions.finishLoading(viewer, ticket)) {
            return;
        }
        OpenMenu menu = notice(blueprint, titleValues, DossierTrouble.of(reply), back);
        if (reply instanceof DossierReply.Found found) {
            try {
                menu = board.apply(found.data());
            } catch (RuntimeException unreadable) {
                log.warn("[Menu] dossier answer could not be shown: " + unreadable);
            }
        }
        sessions.show(viewer, menu);
        canvas.redraw(viewer, menu.render());
    }

    private OpenMenu notice(MenuBlueprint blueprint, TemplateValues titleValues, DossierTrouble trouble,
                            MenuReaction back) {
        return new DossierNoticeMenu(blueprint, painter, titleValues, blueprints.dossierNotice(trouble), back);
    }

    private MenuFrame loading(MenuBlueprint blueprint, TemplateValues titleValues) {
        Map<Integer, ItemFace> items = new HashMap<>();
        items.put(blueprint.middleListSlot(), painter.item(LOADING_MATERIAL, blueprints.loadingName(), List.of(),
                TemplateValues.none()));
        return new MenuFrame(painter.title(blueprint.title(), titleValues), blueprint.size(), items);
    }
}
