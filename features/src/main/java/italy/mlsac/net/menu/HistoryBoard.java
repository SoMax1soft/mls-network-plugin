// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.HistoryEntry;
import italy.mlsac.net.dossier.HistoryPage;
import italy.mlsac.net.text.TemplateValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class HistoryBoard implements OpenMenu {

    static final List<String> FALLBACK_LORE = List.of("&7{MODEL} &8| {PERCENT}", "&7{AGO}");
    static final List<String> SUPPRESSION_LORE = List.of("&7{TRIGGER} &8#{LEVEL}", "&7{ACTIONS}", "&7{AGO}");
    static final String PUNISH_MATERIAL = "IRON_SWORD";
    static final String SUPPRESSION_MATERIAL = "SHIELD";
    static final String EMPTY_MATERIAL = "BARRIER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final HistoryPage history;
    private final HistoryTexts texts;
    private final DossierTimes times;
    private final MenuReaction.OpenHistory request;
    private final List<HistoryEntry> shown;
    private MenuFrame frame;

    HistoryBoard(MenuBlueprint blueprint, FramePainter painter, HistoryPage history, HistoryTexts texts,
                 DossierTimes times, MenuReaction.OpenHistory request) {
        this.blueprint = blueprint;
        this.painter = painter;
        this.history = history;
        this.texts = texts;
        this.times = times;
        this.request = request;
        this.shown = history.entries().subList(0, Math.min(history.entries().size(), blueprint.listSlots().size()));
    }

    @Override
    public MenuFrame render() {
        Map<Integer, ItemFace> items = new HashMap<>();
        List<Integer> slots = blueprint.listSlots();
        for (int position = 0; position < shown.size(); position++) {
            items.put(slots.get(position), entry(shown.get(position)));
        }
        if (shown.isEmpty()) {
            items.put(blueprint.middleListSlot(), painter.item(EMPTY_MATERIAL, texts.emptyName(), List.of(),
                    TemplateValues.none()));
        }
        TemplateValues shared = TemplateValues.of("{PLAYER}", request.player());
        PageSpan span = span();
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
        if (button.isEmpty()) {
            return new MenuReaction.Nothing();
        }
        PageSpan span = span();
        return switch (button.get().action()) {
            case PREV_PAGE -> span.hasPrevious() ? request.turnedTo(history.page() - 1) : new MenuReaction.Nothing();
            case NEXT_PAGE -> span.hasMore() ? request.turnedTo(history.page() + 1) : new MenuReaction.Nothing();
            case BACK -> request.back();
            case CLOSE -> new MenuReaction.Close();
            default -> new MenuReaction.Nothing();
        };
    }

    private PageSpan span() {
        return new PageSpan(history.page(), history.pages(), history.total(), history.hasMore());
    }

    private ItemFace entry(HistoryEntry entry) {
        TemplateValues values = DossierShades.entryValues(entry, times).and("{PLAYER}", request.player());
        return switch (entry.kind()) {
            case HistoryEntry.PUNISH -> face(PUNISH_MATERIAL, texts.punish().orLore(FALLBACK_LORE), values);
            case HistoryEntry.SUPPRESSION -> face(SUPPRESSION_MATERIAL, texts.suppression().orLore(SUPPRESSION_LORE),
                    values);
            default -> face(ValueShade.of(entry.probability()).pane(), texts.detection().orLore(FALLBACK_LORE), values);
        };
    }

    private ItemFace face(String material, EntryText text, TemplateValues values) {
        return painter.item(material, text.name(), text.lore(), values);
    }
}
