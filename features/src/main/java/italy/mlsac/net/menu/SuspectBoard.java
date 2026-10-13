// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.TemplateValues;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SuspectBoard implements OpenMenu {

    static final List<String> FALLBACK_LORE = List.of("&7Mean score {AVG_PROB}", "&7Newest {HISTORY_SIZE} scores",
            "{HISTORY}", "&eLeft click &7- go to the player", "&eRight click &7- every check");
    static final String LOADING_MATERIAL = "SUNFLOWER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final String viewerName;
    private final List<SuspectRow> suspects;
    private final SuspectPresence presence;
    private final boolean kicksShown;
    private int page;
    private MenuFrame frame;

    public SuspectBoard(MenuBlueprint blueprint, FramePainter painter, String viewerName, List<SuspectRow> onlineOrder,
                        SuspectPresence presence, boolean kicksShown) {
        this.presence = presence;
        this.kicksShown = kicksShown;
        this.blueprint = blueprint;
        this.painter = painter;
        this.viewerName = viewerName;
        this.suspects = onlineOrder.stream().filter(row -> !row.lastScores().isEmpty())
                .sorted(Comparator.comparingDouble(SuspectRow::average).reversed()).toList();
    }

    public static MenuFrame loading(MenuBlueprint blueprint, FramePainter painter, String loadingName) {
        Map<Integer, ItemFace> items = new HashMap<>();
        items.put(blueprint.middleListSlot(), painter.item(LOADING_MATERIAL, loadingName, List.of(), TemplateValues.none()));
        return new MenuFrame(painter.title(blueprint.title(), TemplateValues.none()), blueprint.size(), items);
    }

    public int pages() {
        return (suspects.size() + blueprint.slotsPerSheet() - 1) / blueprint.slotsPerSheet();
    }

    public int page() {
        return page;
    }

    @Override
    public MenuFrame render() {
        page = Math.max(0, Math.min(page, Math.max(0, pages() - 1)));
        Map<Integer, ItemFace> items = new HashMap<>();
        List<Integer> slots = blueprint.listSlots();
        int first = page * blueprint.slotsPerSheet();
        for (int position = 0; position < slots.size() && first + position < suspects.size(); position++) {
            items.put(slots.get(position), entry(suspects.get(first + position)));
        }
        for (MenuButton button : blueprint.buttons()) {
            buttonValues(button).ifPresent(values -> painter.button(items, button, values));
        }
        painter.fill(items, blueprint);
        frame = new MenuFrame(painter.title(blueprint.title(), TemplateValues.none()), blueprint.size(), items);
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
        int index = page * blueprint.slotsPerSheet() + position;
        if (position < 0 || index >= suspects.size()) {
            return new MenuReaction.Nothing();
        }
        SuspectRow row = suspects.get(index);
        if (!presence.online(row.player())) {
            return new MenuReaction.Tell(presence.offlineNotice());
        }
        return click.opensChecks() ? new MenuReaction.OpenChecks(row.player())
                : new MenuReaction.CloseAndTeleport(row.player());
    }

    private MenuReaction pressed(MenuButton button) {
        return switch (button.action()) {
            case PREV_PAGE -> turn(page > 0 ? page - 1 : page);
            case NEXT_PAGE -> turn(hasNextPage() ? page + 1 : page);
            case CLOSE -> new MenuReaction.Close();
            case COMMAND -> new MenuReaction.CloseAndRun(ButtonCommand.prepare(button.command(), viewerName));
            case KICKS -> kicksShown ? new MenuReaction.OpenKicks(0) : new MenuReaction.Nothing();
            default -> new MenuReaction.Nothing();
        };
    }

    private MenuReaction turn(int target) {
        if (target == page) {
            return new MenuReaction.Nothing();
        }
        page = target;
        return new MenuReaction.Redraw(render());
    }

    private boolean hasNextPage() {
        return (page + 1) * blueprint.slotsPerSheet() < suspects.size();
    }

    private Optional<TemplateValues> buttonValues(MenuButton button) {
        return switch (button.action()) {
            case PREV_PAGE -> page > 0 ? Optional.of(TemplateValues.of("{PAGE}", page)) : Optional.empty();
            case NEXT_PAGE -> hasNextPage() ? Optional.of(TemplateValues.of("{PAGE}", page + 2)) : Optional.empty();
            case KICKS -> kicksShown ? Optional.of(TemplateValues.of("{PLAYER}", viewerName)) : Optional.empty();
            default -> Optional.of(TemplateValues.of("{CURRENT}", page + 1)
                    .and("{TOTAL}", Math.max(1, pages()))
                    .and("{PAGE}", page + 1)
                    .and("{COUNT}", suspects.size())
                    .and("{PLAYER}", viewerName));
        };
    }

    private ItemFace entry(SuspectRow row) {
        List<Double> history = row.lastTen();
        TemplateValues values = TemplateValues.of("{PLAYER}", row.name())
                .and("{AVG_PROB}", ValueShade.twoDecimals(row.average()))
                .and("{DETECTIONS}", "&7N/A")
                .and("{HISTORY_SIZE}", history.size())
                .and("{HISTORY}", history.stream().map(ValueShade::twoDecimals).collect(Collectors.joining(" ")));
        List<String> lore = blueprint.headLoreLines().isEmpty() ? FALLBACK_LORE : blueprint.headLoreLines();
        return painter.item(ItemFace.PLAYER_HEAD, blueprint.headTitle(), lore, values);
    }
}
