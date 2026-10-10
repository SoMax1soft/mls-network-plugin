// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.messages.BundledTexts;
import italy.mlsac.net.text.LanguageId;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class BlueprintSource {

    static final int BUILT_IN_SIZE = 54;
    static final List<Integer> BUILT_IN_LIST = IntStream.rangeClosed(0, 44).boxed().toList();
    static final String DEFAULT_FILLER = "BLACK_STAINED_GLASS_PANE";

    private final BundledTexts menuFiles;
    private final Supplier<LanguageId> serverLanguage;

    public BlueprintSource(BundledTexts menuFiles, Supplier<LanguageId> serverLanguage) {
        this.menuFiles = menuFiles;
        this.serverLanguage = serverLanguage;
    }

    public MenuBlueprint suspects(Optional<PresetMenu> preset) {
        String title = text("gui.title", "&8MLSAC suspect list");
        String headTitle = text("gui.items.suspect_head.name", "&f{PLAYER}");
        List<String> headLoreLines = lines("gui.items.suspect_head.lore");
        List<MenuButton> builtInButtons = List.of(
                shipped(48, ButtonAction.PREV_PAGE, "gui.items.previous_page", "ARROW", "&7« Back &8({PAGE})", List.of()),
                shipped(49, ButtonAction.PAGE_INFO, "gui.items.page_info", "COMPASS", "&fPage &7{CURRENT}&8/&7{TOTAL}",
                        List.of()),
                shipped(50, ButtonAction.NEXT_PAGE, "gui.items.next_page", "ARROW", "&7Next » &8({PAGE})", List.of()));
        return resolve(preset, title, headTitle, headLoreLines, builtInButtons);
    }

    public MenuBlueprint checks(Optional<PresetMenu> preset) {
        String title = text("gui.checks.title", "&8Scores of {PLAYER}");
        String headTitle = text("gui.checks.pane.name", "&fCheck &7#{INDEX}");
        List<String> headLoreLines = lines("gui.checks.pane.lore");
        List<MenuButton> builtInButtons = List.of(
                shipped(48, ButtonAction.BACK, "gui.checks.back", "ARROW", "&7« Back", List.of()),
                shipped(49, ButtonAction.INFO, "gui.checks.info", "COMPASS", "&f{PLAYER}",
                        linesOrSingle("gui.checks.info.lore", "&7Stored scores: &f{COUNT}")),
                shipped(50, ButtonAction.CLOSE, "gui.checks.exit", "BARRIER", "&cExit", List.of()));
        return resolve(preset, title, headTitle, headLoreLines, builtInButtons);
    }

    public String loadingName() {
        return text("gui.loading", "&ePlease wait");
    }

    public String emptyChecksName() {
        return text("gui.checks.empty.name", "&7Nothing scored so far");
    }

    private MenuBlueprint resolve(Optional<PresetMenu> preset, String builtInTitle, String builtInName,
                                  List<String> builtInLore, List<MenuButton> builtInButtons) {
        if (preset.isEmpty()) {
            return new MenuBlueprint(builtInTitle, BUILT_IN_SIZE, BUILT_IN_LIST, builtInButtons,
                    Optional.of(text("gui.items.filler.material", DEFAULT_FILLER)), builtInName, builtInLore);
        }
        PresetMenu menu = preset.get();
        int rows = Math.max(1, Math.min(6, menu.rows()));
        Optional<String> filler = menu.fillerEnabled()
                ? Optional.of(menu.fillerMaterial().orElse(DEFAULT_FILLER)) : Optional.empty();
        return new MenuBlueprint(menu.title().orElse(builtInTitle), rows * 9, menu.list(), menu.buttons(), filler,
                menu.headTitle().orElse(builtInName), menu.headLoreLines().orElse(builtInLore));
    }

    private MenuButton shipped(int slot, ButtonAction action, String path, String material, String name,
                               List<String> lore) {
        return new MenuButton(slot, action, text(path + ".material", material), text(path + ".name", name), lore, "");
    }

    private String text(String path, String fallback) {
        return menuFiles.nodeText(serverLanguage.get(), path).orElse(fallback);
    }

    private List<String> lines(String path) {
        return menuFiles.lines(serverLanguage.get(), path).orElse(List.of());
    }

    private List<String> linesOrSingle(String path, String fallback) {
        Optional<Object> node = menuFiles.node(serverLanguage.get(), path);
        if (node.isPresent() && node.get() instanceof String single) {
            return List.of(single);
        }
        return menuFiles.lines(serverLanguage.get(), path).orElse(List.of(fallback));
    }
}
