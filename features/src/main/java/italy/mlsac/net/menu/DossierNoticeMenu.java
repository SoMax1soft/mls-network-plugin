// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.TemplateValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DossierNoticeMenu implements OpenMenu {

    static final String NOTICE_MATERIAL = "BARRIER";

    private final MenuBlueprint blueprint;
    private final FramePainter painter;
    private final TemplateValues titleValues;
    private final String notice;
    private final MenuReaction back;
    private MenuFrame frame;

    DossierNoticeMenu(MenuBlueprint blueprint, FramePainter painter, TemplateValues titleValues, String notice,
                      MenuReaction back) {
        this.blueprint = blueprint;
        this.painter = painter;
        this.titleValues = titleValues;
        this.notice = notice;
        this.back = back;
    }

    @Override
    public MenuFrame render() {
        Map<Integer, ItemFace> items = new HashMap<>();
        items.put(blueprint.middleListSlot(), painter.item(NOTICE_MATERIAL, notice, List.of(), titleValues));
        for (MenuButton button : blueprint.buttons()) {
            if (button.action() == ButtonAction.BACK || button.action() == ButtonAction.CLOSE) {
                painter.button(items, button, titleValues);
            }
        }
        painter.fill(items, blueprint);
        frame = new MenuFrame(painter.title(blueprint.title(), titleValues), blueprint.size(), items);
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
            case BACK -> back;
            case CLOSE -> new MenuReaction.Close();
            default -> new MenuReaction.Nothing();
        };
    }
}
