// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.backend.session.LenientJson;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MenuTest implements MenuCanvas {

    private final Set<String> knownMaterials = new HashSet<>(Set.of("ARROW", "COMPASS", "BARRIER", "PAPER",
            "BLACK_STAINED_GLASS_PANE", "PLAYER_HEAD", "SUNFLOWER", "RED_STAINED_GLASS_PANE",
            "ORANGE_STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE",
            "GREEN_STAINED_GLASS_PANE", "DIAMOND"));
    private final FramePainter painter = new FramePainter(BundledResources.colors(), knownMaterials::contains);
    private final BlueprintSource english = new BlueprintSource(BundledResources.menus(), () -> LanguageId.EN);
    private final List<SuspectRow> online = new ArrayList<>();
    private final Set<UUID> offline = new HashSet<>();
    private final List<String> events = new ArrayList<>();
    private final Map<UUID, MenuFrame> screens = new HashMap<>();
    private final RecordingLog log = new RecordingLog();
    private MenuLayouts layouts = MenuLayouts.shipped();
    private final UUID viewer = UUID.randomUUID();
    private final List<String> asked = new ArrayList<>();
    private final Map<String, String> answers = new HashMap<>();
    private DossierReply refusal;
    private CompletableFuture<DossierReply> pending;
    private DossierSettings dossierSettings = DossierSettings.pluginDefaults();
    private long now = 1_760_200_000_000L;

    private static SuspectRow row(String name, double... checks) {
        List<Double> values = new ArrayList<>();
        for (double check : checks) {
            values.add(check);
        }
        double average = values.subList(Math.max(0, values.size() - 10), values.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        return new SuspectRow(UUID.nameUUIDFromBytes(name.getBytes()), name, average, values);
    }

    private MenuDesk desk() {
        return new MenuDesk(english, () -> layouts, painter, this, BundledResources.english(), log,
                new DossierDesk(this::answer, () -> now), () -> dossierSettings,
                new DossierTimes(() -> now, ZoneOffset.UTC));
    }

    private CompletableFuture<DossierReply> answer(DossierQuery query) {
        asked.add(query.path());
        if (pending != null) {
            return pending;
        }
        if (refusal != null) {
            return CompletableFuture.completedFuture(refusal);
        }
        String kind = query.path().substring("plugin/dossier/".length(), query.path().indexOf('?'));
        return CompletableFuture.completedFuture(new DossierReply.Found(LenientJson.object(answers.get(kind))
                .orElseThrow()));
    }

    @Test
    void fiftySuspectsPageThroughTheBuiltInLayout() {
        for (int i = 0; i < 50; i++) {
            online.add(row("P" + i, i / 100.0 + 0.01));
        }
        MenuDesk desk = desk();
        desk.openSuspects(viewer, "Mod");
        MenuFrame first = screens.get(viewer);
        assertEquals(54, first.size());
        assertTrue(first.at(0).orElseThrow().name().contains("P49"));
        assertTrue(first.at(44).orElseThrow().name().contains("P5"));
        assertTrue(first.at(50).orElseThrow().name().contains("(2)"));
        assertEquals("BLACK_STAINED_GLASS_PANE", first.at(48).orElseThrow().material());
        assertEquals(" ", first.at(48).orElseThrow().name());
        assertTrue(first.at(49).orElseThrow().name().contains("1"));
        desk.clicked(viewer, "Mod", 50, ClickKind.LEFT);
        MenuFrame second = screens.get(viewer);
        assertTrue(second.at(4).orElseThrow().name().contains("P0"));
        assertTrue(second.at(5).isEmpty());
        assertTrue(second.at(48).orElseThrow().name().contains("(1)"));
        assertEquals("BLACK_STAINED_GLASS_PANE", second.at(50).orElseThrow().material());
        desk.clicked(viewer, "Mod", 48, ClickKind.LEFT);
        assertTrue(screens.get(viewer).at(0).orElseThrow().name().contains("P49"));
    }

    @Test
    void loadingItemShownFirstInMiddleListSlot() {
        MenuFrame loading = SuspectBoard.loading(english.suspects(Optional.empty()), painter, english.loadingName());
        assertEquals("SUNFLOWER", loading.at(22).orElseThrow().material());
        assertEquals("§7Loading...", loading.at(22).orElseThrow().name());
    }

    @Test
    void presetListSlotsAreSortedAndRowsClamped() {
        online.add(row("A", 0.9));
        online.add(row("B", 0.5));
        online.add(row("C", 0.7));
        layouts = new MenuLayouts(Optional.of(new PresetMenu(Optional.of("&8Custom"), 3, true, Optional.empty(),
                List.of(20, 10, 11, 10, 40), Optional.empty(), Optional.empty(), List.of(
                        new MenuButton(10, ButtonAction.CLOSE, "BARRIER", "x", List.of(), ""),
                        new MenuButton(26, ButtonAction.CLOSE, "FOO_BAR", "close", List.of(), "")))), Optional.empty());
        desk().openSuspects(viewer, "Mod");
        MenuFrame frame = screens.get(viewer);
        assertEquals(27, frame.size());
        assertTrue(frame.at(10).orElseThrow().name().contains("A"));
        assertTrue(frame.at(11).orElseThrow().name().contains("C"));
        assertTrue(frame.at(20).orElseThrow().name().contains("B"));
        assertEquals("PAPER", frame.at(26).orElseThrow().material());
        assertEquals("§8Custom", frame.title());
    }

    @Test
    void suspectEntryPlaceholdersAndFallbackLore() {
        online.add(row("Steve", 0.95, 0.5, 0.81));
        layouts = new MenuLayouts(Optional.of(new PresetMenu(Optional.empty(), 6, false, Optional.empty(),
                List.of(0), Optional.of("&f{PLAYER}"), Optional.of(List.of()), List.of())), Optional.empty());
        desk().openSuspects(viewer, "Mod");
        ItemFace head = screens.get(viewer).at(0).orElseThrow();
        assertEquals("PLAYER_HEAD", head.material());
        assertEquals("§fSteve", head.name());
        assertEquals(List.of("§7Mean score §60.75§r", "§7Newest 3 scores", "§40.95§r §e0.50§r §c0.81§r",
                "§eLeft click §7- go to the player", "§eRight click §7- every check"), head.lore());
        assertTrue(screens.get(viewer).at(1).isEmpty());
    }

    @Test
    void clicksOnHeadsTeleportOrOpenChecksOrReportOffline() {
        SuspectRow steve = row("Steve", 0.3, 0.95);
        online.add(steve);
        MenuDesk desk = desk();
        desk.openSuspects(viewer, "Mod");
        desk.clicked(viewer, "Mod", 0, ClickKind.RIGHT);
        MenuFrame checks = screens.get(viewer);
        assertEquals("RED_STAINED_GLASS_PANE", checks.at(0).orElseThrow().material());
        assertEquals("GREEN_STAINED_GLASS_PANE", checks.at(1).orElseThrow().material());
        assertTrue(checks.title().contains("Steve"));
        desk.clicked(viewer, "Mod", 48, ClickKind.LEFT);
        desk.clicked(viewer, "Mod", 0, ClickKind.LEFT);
        assertEquals("teleport " + steve.player(), events.get(events.size() - 1));
        desk.openSuspects(viewer, "Mod");
        offline.add(steve.player());
        desk.clicked(viewer, "Mod", 0, ClickKind.SHIFT_RIGHT);
        assertEquals("send §cPlayer offline", events.get(events.size() - 1));
    }

    @Test
    void checksMenuShowsNewestFirstWithPercentPlaceholders() {
        SuspectRow steve = row("Steve", 0.1, 0.5, 0.97);
        online.add(steve);
        layouts = new MenuLayouts(Optional.empty(), Optional.of(new PresetMenu(Optional.of("&8{PLAYER}"), 1, true,
                Optional.of("DIAMOND"), List.of(0, 1), Optional.of("#{INDEX} {PROB}"), Optional.empty(), List.of(
                new MenuButton(8, ButtonAction.INFO, "COMPASS", "{PLAYER} {COUNT}", List.of(), "")))));
        desk().openChecks(viewer, steve.player());
        MenuFrame frame = screens.get(viewer);
        assertEquals(9, frame.size());
        assertEquals("#1 §40.97§r", frame.at(0).orElseThrow().name());
        assertEquals(List.of("§8▪ §7Chance §8» §497%§r"), frame.at(0).orElseThrow().lore());
        assertEquals("#2 §e0.50§r", frame.at(1).orElseThrow().name());
        assertEquals("Steve 3", frame.at(8).orElseThrow().name());
        assertEquals("DIAMOND", frame.at(5).orElseThrow().material());
    }

    @Test
    void emptyChecksShowBarrier() {
        SuspectRow nobody = new SuspectRow(UUID.randomUUID(), "Nobody", 0, List.of());
        online.add(nobody);
        desk().openChecks(viewer, nobody.player());
        ItemFace barrier = screens.get(viewer).at(22).orElseThrow();
        assertEquals("BARRIER", barrier.material());
        assertEquals("§7No checks", barrier.name());
        assertTrue(screens.get(viewer).at(49).orElseThrow().lore().get(0).contains("0"));
    }

    @Test
    void commandButtonsRunAsConsoleOrAreRefused() {
        SuspectRow steve = row("Steve", 0.9);
        SuspectRow unsafe = row("a b", 0.9);
        online.add(steve);
        online.add(unsafe);
        layouts = new MenuLayouts(Optional.of(new PresetMenu(Optional.empty(), 6, true, Optional.empty(),
                List.of(0, 1), Optional.empty(), Optional.empty(), List.of(
                new MenuButton(53, ButtonAction.COMMAND, "PAPER", "cmd", List.of(), "/say {PLAYER} hi")))),
                Optional.of(new PresetMenu(Optional.empty(), 6, true, Optional.empty(), List.of(0), Optional.empty(),
                        Optional.empty(), List.of(new MenuButton(53, ButtonAction.COMMAND, "PAPER", "ban", List.of(),
                        "ban {PLAYER} test")))));
        MenuDesk desk = desk();
        desk.openSuspects(viewer, "Mod");
        desk.clicked(viewer, "Mod", 53, ClickKind.LEFT);
        assertEquals("console say Mod hi", events.get(events.size() - 1));
        desk.openChecks(viewer, steve.player());
        desk.clicked(viewer, "Mod", 53, ClickKind.LEFT);
        assertEquals("console ban Steve test", events.get(events.size() - 1));
        assertEquals("[Menu] button pressed by Mod dispatched: ban Steve test", log.infos.get(1));
        desk.openChecks(viewer, unsafe.player());
        desk.clicked(viewer, "Mod", 53, ClickKind.LEFT);
        assertEquals("send " + BundledResources.english().text(ButtonCommand.REFUSAL_KEY), events.get(events.size() - 1));
        assertInstanceOf(ButtonCommand.Skip.class, ButtonCommand.prepare(" / ", "x"));
    }

    @Test
    void checksMenuIgnoresListAndPagingClicks() {
        SuspectRow steve = row("Steve", 0.9);
        online.add(steve);
        MenuDesk desk = desk();
        desk.openChecks(viewer, steve.player());
        int before = events.size();
        desk.clicked(viewer, "Mod", 0, ClickKind.LEFT);
        desk.clicked(viewer, "Mod", 30, ClickKind.LEFT);
        assertEquals(before, events.size());
        desk.clicked(viewer, "Mod", 50, ClickKind.LEFT);
        assertEquals("close", events.get(events.size() - 1));
    }

    @Test
    void menuFilesInAllLanguagesResolve() {
        for (LanguageId language : LanguageId.values()) {
            BlueprintSource source = new BlueprintSource(BundledResources.menus(), () -> language);
            MenuBlueprint suspects = source.suspects(Optional.empty());
            assertEquals(IntStream.rangeClosed(0, 44).boxed().toList(), suspects.listSlots());
            assertEquals(4, suspects.buttons().size());
            assertEquals(3, source.checks(Optional.empty()).buttons().size());
            assertEquals(5, source.kicks(Optional.empty()).buttons().size());
            assertEquals(5, source.history().buttons().size());
            assertEquals(2, source.profile(Optional.empty()).buttons().size());
            assertTrue(source.history().title().contains("{PLAYER}"), language.id());
            assertFalse(source.kicks(Optional.empty()).headLoreLines().isEmpty(), language.id());
            assertFalse(source.profile(Optional.empty()).headLoreLines().isEmpty(), language.id());
            assertFalse(source.historyTexts().punish().lore().isEmpty(), language.id());
            assertFalse(source.historyTexts().suppression().lore().isEmpty(), language.id());
            assertFalse(source.profileTexts().punishments().lore().isEmpty(), language.id());
            for (DossierTrouble trouble : DossierTrouble.values()) {
                assertTrue(BundledResources.menus().node(language, "gui.dossier." + trouble.key()).isPresent(),
                        language.id() + " " + trouble);
            }
        }
    }

    @Override
    public void open(UUID target, MenuFrame frame) {
        events.add("open " + frame.title());
        screens.put(target, frame);
    }

    @Override
    public void redraw(UUID target, MenuFrame frame) {
        events.add("redraw");
        screens.put(target, frame);
    }

    @Override
    public void close(UUID target) {
        events.add("close");
    }

    @Override
    public void send(UUID target, String coloredText) {
        events.add("send " + coloredText);
    }

    @Override
    public void teleport(UUID target, UUID suspect) {
        events.add("teleport " + suspect);
    }

    @Override
    public void runConsoleNextTick(String command) {
        events.add("console " + command);
    }

    @Override
    public void onViewerThread(UUID target, Runnable task) {
        task.run();
    }

    @Override
    public List<SuspectRow> onlineSuspects() {
        return List.copyOf(online);
    }

    @Override
    public Optional<SuspectRow> suspect(UUID player) {
        return online.stream().filter(row -> row.player().equals(player)).findFirst();
    }

    @Override
    public boolean online(UUID player) {
        return !offline.contains(player);
    }
}
