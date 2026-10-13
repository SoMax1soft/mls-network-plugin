// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.KickEntry;
import italy.mlsac.net.dossier.KickPage;
import italy.mlsac.net.text.TemplateValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class KicksBoard implements OpenMenu {

    static final List<String> FALLBACK_LORE = List.of("&7{TIME} &8({AGO})", "&7{MODEL} &8| {PERCENT}",
            "&7Kicks &f{KICKS} &8| &7Bans &f{BANS}", "&eClick &7- history");
    static final String EMPTY_MATERIAL = "BARRIER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final KickPage kicks;
    private final DossierTimes times;
    private final String emptyName;
    private final int days;
    private final boolean withSuppressions;
    private final String viewerName;
    private final List<KickEntry> shown;
    private MenuFrame frame;

    KicksBoard(MenuBlueprint blueprint, FramePainter painter, KickPage kicks, DossierTimes times, String emptyName,
               int days, boolean withSuppressions, String viewerName) {
        this.blueprint = blueprint;
        this.painter = painter;
        this.kicks = kicks;
        this.times = times;
        this.emptyName = emptyName;
        this.days = days;
        this.withSuppressions = withSuppressions;
        this.viewerName = viewerName;
        this.shown = kicks.entries().subList(0, Math.min(kicks.entries().size(), blueprint.listSlots().size()));
    }

    @Override
    public MenuFrame render() {
        Map<Integer, ItemFace> items = new HashMap<>();
        List<Integer> slots = blueprint.listSlots();
        for (int position = 0; position < shown.size(); position++) {
            items.put(slots.get(position), entry(shown.get(position)));
        }
        if (shown.isEmpty()) {
            items.put(blueprint.middleListSlot(), painter.item(EMPTY_MATERIAL, emptyName, List.of(),
                    TemplateValues.of("{DAYS}", days)));
        }
        PageSpan span = span();
        TemplateValues shared = TemplateValues.of("{PLAYER}", viewerName).and("{DAYS}", days);
        for (MenuButton button : blueprint.buttons()) {
            span.buttonValues(button.action(), shared).ifPresent(values -> painter.button(items, button, values));
        }
        painter.fill(items, blueprint);
        frame = new MenuFrame(painter.title(blueprint.title(), shared), blueprint.size(), items);
        return frame;
    }

    @Override
    public MenuReaction click(int slot, ClickKind click) {
        if (frame == null || frame.at(slot).isEmpty()) {
            return new MenuReaction.Nothing();
        }
        Optional<MenuButton> button = blueprint.firstButtonAt(slot);
        if (button.isPresent()) {
            return pressed(button.get());
        }
        int position = blueprint.listSlots().indexOf(slot);
        if (position < 0 || position >= shown.size()) {
            return new MenuReaction.Nothing();
        }
        return new MenuReaction.OpenHistory(shown.get(position).player(), 0, withSuppressions,
                new MenuReaction.OpenKicks(kicks.page()));
    }

    private MenuReaction pressed(MenuButton button) {
        PageSpan span = span();
        return switch (button.action()) {
            case PREV_PAGE -> span.hasPrevious() ? new MenuReaction.OpenKicks(kicks.page() - 1)
                    : new MenuReaction.Nothing();
            case NEXT_PAGE -> span.hasMore() ? new MenuReaction.OpenKicks(kicks.page() + 1)
                    : new MenuReaction.Nothing();
            case BACK -> new MenuReaction.OpenSuspects();
            case CLOSE -> new MenuReaction.Close();
            case COMMAND -> new MenuReaction.CloseAndRun(ButtonCommand.prepare(button.command(), viewerName));
            default -> new MenuReaction.Nothing();
        };
    }

    private PageSpan span() {
        return new PageSpan(kicks.page(), kicks.pages(), kicks.total(), kicks.hasMore());
    }

    private ItemFace entry(KickEntry kick) {
        TemplateValues values = TemplateValues.of("{PLAYER}", kick.player())
                .and("{ACTION}", DossierShades.shownAction(kick.action()))
                .and("{TIME}", times.stamp(kick.at()))
                .and("{AGO}", times.ago(kick.at()))
                .and("{MODEL}", DossierShades.shownModel(kick.model()))
                .and("{PERCENT}", ValueShade.roundedPercent(kick.probability()))
                .and("{KICKS}", kick.kicks())
                .and("{BANS}", kick.bans());
        List<String> lore = blueprint.headLoreLines().isEmpty() ? FALLBACK_LORE : blueprint.headLoreLines();
        return painter.item(ItemFace.PLAYER_HEAD, blueprint.headTitle(), lore, values);
    }
}
