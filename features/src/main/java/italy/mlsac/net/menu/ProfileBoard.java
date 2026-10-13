// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.HistoryEntry;
import italy.mlsac.net.dossier.PlayerDossier;
import italy.mlsac.net.text.TemplateValues;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ProfileBoard implements OpenMenu {

    static final int SUPPRESSIONS_SLOT = 47;
    static final int PUNISHMENTS_SLOT = 51;
    static final List<String> FALLBACK_LORE = List.of("&7{PERCENT}", "&7{MODEL}", "&7{TIME} &8({AGO})");
    static final List<String> FALLBACK_PUNISH_LORE = List.of("&7Kicks &f{KICKS} &8| &7Bans &f{BANS}",
            "&7Last &f{LAST_ACTION} &8({LAST_AGO})", "&eClick &7- history");
    static final String EMPTY_MATERIAL = "BARRIER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final PlayerDossier dossier;
    private final ProfileTexts texts;
    private final DossierTimes times;
    private final DossierSettings.ProfileSheet sheet;
    private final String player;
    private final List<HistoryEntry> shown;
    private MenuFrame frame;

    ProfileBoard(MenuBlueprint blueprint, FramePainter painter, PlayerDossier dossier, ProfileTexts texts,
                 DossierTimes times, DossierSettings.ProfileSheet sheet, String player) {
        this.blueprint = blueprint;
        this.painter = painter;
        this.dossier = dossier;
        this.texts = texts;
        this.times = times;
        this.sheet = sheet;
        this.player = player;
        int limit = Math.min(sheet.latest(), blueprint.listSlots().size());
        this.shown = dossier.latest().subList(0, Math.min(limit, dossier.latest().size()));
    }

    @Override
    public MenuFrame render() {
        Map<Integer, ItemFace> items = new HashMap<>();
        List<Integer> slots = blueprint.listSlots();
        for (int position = 0; position < shown.size(); position++) {
            items.put(slots.get(position), entry(position + 1, shown.get(position)));
        }
        if (shown.isEmpty()) {
            items.put(blueprint.middleListSlot(), painter.item(EMPTY_MATERIAL, texts.emptyName(), List.of(),
                    TemplateValues.none()));
        }
        TemplateValues header = header();
        for (MenuButton button : blueprint.buttons()) {
            if (button.action() == ButtonAction.INFO && !dossier.known()) {
                items.put(button.slot(), painter.item(button.material(), button.name(), List.of(texts.unknownPlayer()),
                        header));
            } else {
                painter.button(items, button, header);
            }
        }
        if (sheet.suppressions() && free(SUPPRESSIONS_SLOT)) {
            items.put(SUPPRESSIONS_SLOT, suppressions(header));
        }
        if (sheet.punishments() && free(PUNISHMENTS_SLOT)) {
            EntryText text = texts.punishments().orLore(FALLBACK_PUNISH_LORE);
            items.put(PUNISHMENTS_SLOT, painter.item(texts.punishmentsMaterial(), text.name(), text.lore(), header));
        }
        painter.fill(items, blueprint);
        frame = new MenuFrame(painter.title(blueprint.title(), header), blueprint.size(), items);
        return frame;
    }

    @Override
    public MenuReaction click(int slot, ClickKind click) {
        if (frame == null || frame.at(slot).isEmpty()) {
            return new MenuReaction.Nothing();
        }
        Optional<MenuButton> button = blueprint.firstButtonAt(slot);
        if (button.isPresent()) {
            return switch (button.get().action()) {
                case CLOSE, BACK -> new MenuReaction.Close();
                case KICKS -> new MenuReaction.OpenKicks(0);
                case COMMAND -> new MenuReaction.CloseAndRun(ButtonCommand.prepare(button.get().command(), player));
                default -> new MenuReaction.Nothing();
            };
        }
        if (slot == PUNISHMENTS_SLOT && sheet.punishments() && free(slot)) {
            return new MenuReaction.OpenHistory(player, 0, sheet.suppressions(), new MenuReaction.OpenProfile(player));
        }
        if (slot == SUPPRESSIONS_SLOT && sheet.suppressions() && free(slot)) {
            return new MenuReaction.OpenHistory(player, 0, true, new MenuReaction.OpenProfile(player));
        }
        return new MenuReaction.Nothing();
    }

    private boolean free(int slot) {
        return slot < blueprint.size() && !blueprint.listSlots().contains(slot)
                && blueprint.firstButtonAt(slot).isEmpty();
    }

    private ItemFace entry(int index, HistoryEntry detection) {
        TemplateValues values = DossierShades.entryValues(detection, times).and("{INDEX}", index)
                .and("{PLAYER}", player);
        List<String> lore = blueprint.headLoreLines().isEmpty() ? FALLBACK_LORE : blueprint.headLoreLines();
        return painter.item(ValueShade.of(detection.probability()).pane(), blueprint.headTitle(), lore, values);
    }

    private ItemFace suppressions(TemplateValues header) {
        List<String> lore = new ArrayList<>(texts.suppressions().lore());
        for (HistoryEntry suppression : dossier.suppressions()) {
            lore.add(DossierShades.entryValues(suppression, times).apply(texts.suppressionLine()));
        }
        return painter.item(texts.suppressionsMaterial(), texts.suppressions().name(), lore, header);
    }

    private TemplateValues header() {
        return ProfileValues.of(dossier, player, times);
    }
}
