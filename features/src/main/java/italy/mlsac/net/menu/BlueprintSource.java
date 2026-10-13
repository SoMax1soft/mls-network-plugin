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
    static final List<Integer> PROFILE_LIST = IntStream.rangeClosed(9, 36).boxed().toList();
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
                shipped(50, ButtonAction.NEXT_PAGE, "gui.items.next_page", "ARROW", "&7Next » &8({PAGE})", List.of()),
                shipped(53, ButtonAction.KICKS, "gui.items.kicks", "IRON_SWORD", "&cKicks",
                        linesOrSingle("gui.items.kicks.lore", "&7Latest kicks and bans")));
        return resolve(preset, title, headTitle, headLoreLines, builtInButtons, BUILT_IN_LIST);
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
        return resolve(preset, title, headTitle, headLoreLines, builtInButtons, BUILT_IN_LIST);
    }

    public MenuBlueprint kicks(Optional<PresetMenu> preset) {
        String title = text("gui.kicks.title", "&8MLSAC kicks");
        String headTitle = text("gui.kicks.entry.name", "&f{PLAYER} &8- &c{ACTION}");
        List<String> headLoreLines = lines("gui.kicks.entry.lore");
        return resolve(preset, title, headTitle, headLoreLines, pagedButtons("gui.kicks", "&7« Players"),
                BUILT_IN_LIST);
    }

    public MenuBlueprint history() {
        return resolve(Optional.empty(), text("gui.history.title", "&8History of {PLAYER}"), "", List.of(),
                pagedButtons("gui.history", "&7« Back"), BUILT_IN_LIST);
    }

    public MenuBlueprint profile(Optional<PresetMenu> preset) {
        String title = text("gui.profile.title", "&8Profile of {PLAYER}");
        String headTitle = text("gui.profile.entry.name", "&fDetection &7#{INDEX}");
        List<String> headLoreLines = lines("gui.profile.entry.lore");
        List<MenuButton> builtInButtons = List.of(
                shipped(4, ButtonAction.INFO, "gui.profile.info", ItemFace.PLAYER_HEAD, "&f{PLAYER}",
                        linesOrSingle("gui.profile.info.lore", "&7Detections: &f{TOTAL}")),
                shipped(49, ButtonAction.CLOSE, "gui.profile.exit", "BARRIER", "&cExit", List.of()));
        return resolve(preset, title, headTitle, headLoreLines, builtInButtons, PROFILE_LIST);
    }

    public HistoryTexts historyTexts() {
        return new HistoryTexts(
                new EntryText(text("gui.history.punish.name", "&c{ACTION} &8- &f{TIME}"),
                        lines("gui.history.punish.lore")),
                new EntryText(text("gui.history.suppression.name", "&eSuppression &8- &f{TIME}"),
                        lines("gui.history.suppression.lore")),
                new EntryText(text("gui.history.detection.name", "&fDetection &8- &f{TIME}"),
                        lines("gui.history.detection.lore")),
                text("gui.history.empty.name", "&7Nothing stored"));
    }

    public ProfileTexts profileTexts() {
        return new ProfileTexts(
                new EntryText(text("gui.profile.suppressions.name", "&eSuppressions &7({SUPPRESSIONS})"),
                        lines("gui.profile.suppressions.lore")),
                text("gui.profile.suppressions.material", "SHIELD"),
                text("gui.profile.suppressions.line", "&8▪ &f{AGO} &7{TRIGGER} &8#{LEVEL} &8» &f{ACTIONS}"),
                new EntryText(text("gui.profile.punishments.name", "&cPunishments &7({PUNISHMENTS})"),
                        lines("gui.profile.punishments.lore")),
                text("gui.profile.punishments.material", "IRON_SWORD"),
                text("gui.profile.unknown", "&7Never seen here"),
                text("gui.profile.empty.name", "&7No detections stored"));
    }

    public String loadingName() {
        return text("gui.loading", "&ePlease wait");
    }

    public String emptyChecksName() {
        return text("gui.checks.empty.name", "&7Nothing scored so far");
    }

    public String emptyKicksName() {
        return text("gui.kicks.empty.name", "&7No kicks in the last {DAYS} days");
    }

    public String dossierNotice(DossierTrouble trouble) {
        return text("gui.dossier." + trouble.key(), trouble.fallback());
    }

    private List<MenuButton> pagedButtons(String root, String backName) {
        return List.of(
                shipped(45, ButtonAction.BACK, root + ".back", "ARROW", backName, List.of()),
                shipped(48, ButtonAction.PREV_PAGE, root + ".previous_page", "ARROW", "&7« Back &8({PAGE})", List.of()),
                shipped(49, ButtonAction.PAGE_INFO, root + ".page_info", "COMPASS", "&fPage &7{CURRENT}&8/&7{TOTAL}",
                        List.of()),
                shipped(50, ButtonAction.NEXT_PAGE, root + ".next_page", "ARROW", "&7Next » &8({PAGE})", List.of()),
                shipped(53, ButtonAction.CLOSE, root + ".exit", "BARRIER", "&cExit", List.of()));
    }

    private MenuBlueprint resolve(Optional<PresetMenu> preset, String builtInTitle, String builtInName,
                                  List<String> builtInLore, List<MenuButton> builtInButtons, List<Integer> builtInList) {
        if (preset.isEmpty()) {
            return new MenuBlueprint(builtInTitle, BUILT_IN_SIZE, builtInList, builtInButtons,
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
