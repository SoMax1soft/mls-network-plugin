// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.TemplateValues;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChecksMenu implements OpenMenu {

    static final List<String> FALLBACK_LORE = List.of("&7Score {PERCENT}");
    static final String EMPTY_MATERIAL = "BARRIER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final SuspectRow suspect;
    private final String emptyName;
    private MenuFrame frame;

    public ChecksMenu(MenuBlueprint blueprint, FramePainter painter, SuspectRow suspect, String emptyName) {
        this.blueprint = blueprint;
        this.painter = painter;
        this.suspect = suspect;
        this.emptyName = emptyName;
    }

    @Override
    public MenuFrame render() {
        Map<Integer, ItemFace> items = new HashMap<>();
        List<Double> newestFirst = new ArrayList<>(suspect.lastScores());
        Collections.reverse(newestFirst);
        List<Integer> slots = blueprint.listSlots();
        int count = Math.min(newestFirst.size(), slots.size());
        for (int i = 1; i <= count; i++) {
            items.put(slots.get(i - 1), entry(i, newestFirst.get(i - 1)));
        }
        if (newestFirst.isEmpty()) {
            items.put(blueprint.middleListSlot(), painter.item(EMPTY_MATERIAL, emptyName, List.of(), TemplateValues.none()));
        }
        TemplateValues buttonValues = TemplateValues.of("{PLAYER}", suspect.name()).and("{COUNT}", newestFirst.size());
        blueprint.buttons().forEach(button -> painter.button(items, button, buttonValues));
        painter.fill(items, blueprint);
        frame = new MenuFrame(painter.title(blueprint.title(), TemplateValues.of("{PLAYER}", suspect.name())),
                blueprint.size(), items);
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
        return switch (button.get().action()) {
            case BACK -> new MenuReaction.OpenSuspects();
            case CLOSE -> new MenuReaction.Close();
            case COMMAND -> new MenuReaction.CloseAndRun(ButtonCommand.prepare(button.get().command(), suspect.name()));
            case KICKS -> new MenuReaction.OpenKicks(0);
            default -> new MenuReaction.Nothing();
        };
    }

    private ItemFace entry(int index, double value) {
        TemplateValues values = TemplateValues.of("{INDEX}", index)
                .and("{PERCENT}", ValueShade.roundedPercent(value))
                .and("{PROB}", ValueShade.twoDecimals(value))
                .and("{PLAYER}", suspect.name());
        List<String> lore = blueprint.headLoreLines().isEmpty() ? FALLBACK_LORE : blueprint.headLoreLines();
        return painter.item(ValueShade.of(value).pane(), blueprint.headTitle(), lore, values);
    }
}
